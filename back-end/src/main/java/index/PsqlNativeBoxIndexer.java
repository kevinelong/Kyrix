package index;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import main.Config;
import main.DbConnector;
import main.Main;
import project.Canvas;
import project.Layer;
import project.Transform;
import box.Box;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by wenbo on 12/30/18.
 */
public class PsqlNativeBoxIndexer extends Indexer {

    private static PsqlNativeBoxIndexer instance = null;
    private static boolean isCitus = false;

    // singleton pattern to ensure only one instance existed
    private PsqlNativeBoxIndexer(boolean isCitus) { this.isCitus = isCitus; }

    // thread-safe instance getter
    public static synchronized PsqlNativeBoxIndexer getInstance(boolean isCitus) {

        if (instance == null)
            instance = new PsqlNativeBoxIndexer(isCitus);
        return instance;
    }

    @Override
    public void createMV(Canvas c, int layerId) throws Exception {

        Statement bboxStmt = DbConnector.getStmtByDbName(Config.databaseName);
        Connection dbConn = DbConnector.getDbConn(Config.dbServer, Config.databaseName, Config.userName, Config.password);

        Layer l = c.getLayers().get(layerId);
        Transform trans = l.getTransform();

        // step 0: create tables for storing bboxes and tiles
        String bboxTableName = "bbox_" + Main.getProject().getName() + "_" + c.getId() + "layer" + layerId;

        // drop table if exists
        String sql = "drop table if exists " + bboxTableName + ";";
        System.out.println(sql);
        bboxStmt.executeUpdate(sql);

        // create the bbox table
        sql = "create table " + bboxTableName + " (";
        for (int i = 0; i < trans.getColumnNames().size(); i ++)
            sql += trans.getColumnNames().get(i) + " text, ";
	if (isCitus) {
	    sql += "citus_distribution_id int, ";
	}
        sql += "cx double precision, cy double precision, minx double precision, miny double precision, maxx double precision, maxy double precision, geom box)";
        System.out.println(sql);
        bboxStmt.executeUpdate(sql);

        // if this is an empty layer, return
        if (trans.getDb().equals(""))
            return ;

        // step 1: set up nashorn environment for running javascript code
        NashornScriptEngine engine = null;
        if (! trans.getTransformFunc().equals(""))
            engine = setupNashorn(trans.getTransformFunc());

        // step 2: looping through query results
        // TODO: distinguish between separable and non-separable cases
        Statement rawDBStmt = DbConnector.getStmtByDbName(trans.getDb());
        ResultSet rs = DbConnector.getQueryResultIterator(rawDBStmt, trans.getQuery());
        int numColumn = rs.getMetaData().getColumnCount();
        int rowCount = 0;
        String insertSql = "insert into " + bboxTableName + " values (";
        for (int i = 0; i < trans.getColumnNames().size() + 6; i ++)
            insertSql += "?, ";
	if (isCitus) {
	    insertSql += "?, ";
	}
	insertSql += "box( point(?,?), point(?,?) ) );";
        System.out.println(insertSql);
        PreparedStatement preparedStmt = dbConn.prepareStatement(insertSql);
	long startTs = (new Date()).getTime();
	long lastTs = startTs;
        while (rs.next()) {

            // count log
            rowCount ++;
            if (rowCount % 1000 == 0) {
                long currTs = (new Date()).getTime();
		if (currTs/5000 > lastTs/5000) {
                    lastTs = currTs;
                    System.out.println((currTs-startTs)/1000 + " secs: "+rowCount+" records inserted. "+1000*rowCount/(currTs-startTs)+" recs/sec.");
		}
            }

            // get raw row
            ArrayList<String> curRawRow = new ArrayList<>();
            for (int i = 1; i <= numColumn; i ++)
                curRawRow.add(rs.getString(i));

            // step 3: run transform function on this tuple
            ArrayList<String> transformedRow;
            if (! trans.getTransformFunc().equals(""))
                transformedRow = getTransformedRow(c, curRawRow, engine);
            else
                transformedRow = curRawRow;

            // step 4: calculate bounding boxes
            ArrayList<Double> curBbox = getBboxCoordinates(c, l, transformedRow);

            // insert into bbox table
	    int pscol = 1;
            for (int i = 0; i < transformedRow.size(); i ++)
                preparedStmt.setString(pscol++, transformedRow.get(i).replaceAll("\'", "\'\'"));
	    if (isCitus) {
		// row number is a fine distribution key (for now) - round robin across the cluster
		preparedStmt.setInt(pscol++, rowCount);
	    }
            for (int i = 0; i < 6; i ++)
                preparedStmt.setDouble(pscol++, curBbox.get(i));

            double minx, miny, maxx, maxy;
            minx = curBbox.get(2);
            miny = curBbox.get(3);
            maxx = curBbox.get(4);
            maxy = curBbox.get(5);
	    preparedStmt.setDouble(pscol++, minx);
	    preparedStmt.setDouble(pscol++, miny);
	    preparedStmt.setDouble(pscol++, maxx);
	    preparedStmt.setDouble(pscol++, maxy);
            preparedStmt.addBatch();

            if (rowCount % Config.bboxBatchSize == 0) {
                preparedStmt.executeBatch();
                DbConnector.commitConnection(Config.databaseName);
            }
        }
        rs.close();
        rawDBStmt.close();
        DbConnector.closeConnection(trans.getDb());

        // insert tail stuff
        if (rowCount % Config.bboxBatchSize != 0) {
            preparedStmt.executeBatch();
            DbConnector.commitConnection(Config.databaseName);
        }
        preparedStmt.close();

	// for performance distribute the citus table after loading
	if (isCitus) {
	    sql = "SELECT create_distributed_table('"+bboxTableName+"', 'citus_distribution_id');";
	    System.out.println(sql);
	    bboxStmt.executeQuery(sql);
	}
	
        // create index
	// TODO: consider sp-gist
        sql = "create index sp_" + bboxTableName + " on " + bboxTableName + " using gist (geom);";
        bboxStmt.executeUpdate(sql);
        sql = "cluster " + bboxTableName + " using sp_" + bboxTableName + ";";
        bboxStmt.executeUpdate(sql);
        DbConnector.commitConnection(Config.databaseName);
        bboxStmt.close();
    }

    @Override
    public ArrayList<ArrayList<String>> getDataFromRegion(Canvas c, int layerId, String regionWKT, String predicate, Box newBox, Box oldBox)
            throws Exception {

        // get column list string
        String colListStr = c.getLayers().get(layerId).getTransform().getColStr("");

        // construct range query
        String sql = "select " + colListStr + " from bbox_" + Main.getProject().getName() + "_"
	    + c.getId() + "layer" + layerId + " where ";
	sql += "geom && box('"+newBox.getCSV()+"')";
	sql += "and not (geom && box('"+oldBox.getCSV()+"') )";
        if (predicate.length() > 0)
            sql += " and " + predicate + ";";
        System.out.println(sql);

        // return
        return DbConnector.getQueryResult(Config.databaseName, sql);
    }

    @Override
    public ArrayList<ArrayList<String>> getDataFromTile(Canvas c, int layerId, int minx, int miny, String predicate)
            throws Exception {

        // get column list string
        String colListStr = c.getLayers().get(layerId).getTransform().getColStr("");

        // construct range query
        String sql = "select " + colListStr + " from bbox_" + Main.getProject().getName() + "_"
	    + c.getId() + "layer" + layerId + " where ";
	String boxStr = "geom && box( '"+minx + "," + miny + "," + (minx + Config.tileW) + "," + (miny+Config.tileH) + "')";
	sql += boxStr;
        if (predicate.length() > 0)
            sql += " and " + predicate;
	sql += ";";
        System.out.println(boxStr +" : " + sql);

        // return
        return DbConnector.getQueryResult(Config.databaseName, sql);
    }
}