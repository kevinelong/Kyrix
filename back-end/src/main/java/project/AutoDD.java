package project;

import main.DbConnector;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Created by wenbo on 3/31/19.
 */
public class AutoDD {

    private String query, db;
    private String xCol, yCol;
    private int bboxW, bboxH;
    private String rendering;
    private ArrayList<String> columnNames, queriedColumnNames = null;
    private int numLevels, topLevelWidth, topLevelHeight, zoomFactor;
    private int xColId = -1, yColId = -1;
    private double loX = Double.NaN, loY, hiX, hiY;

    public String getQuery() {
        return query;
    }

    public String getDb() {
        return db;
    }

    public String getxCol() {
        return xCol;
    }

    public String getyCol() {
        return yCol;
    }

    public int getBboxW() {
        return bboxW;
    }

    public int getBboxH() {
        return bboxH;
    }

    public String getRendering() {
        return rendering;
    }

    public int getXColId() {

        if (xColId < 0) {
            ArrayList<String> colNames = this.getColumnNames();
            for (int i = 0; i < colNames.size(); i ++)
                if (colNames.get(i).equals(xCol))
                    xColId = i;
        }
        return xColId;
    }

    public int getYColId() {

        if (yColId < 0) {
            ArrayList<String> colNames = this.getColumnNames();
            for (int i = 0; i < colNames.size(); i ++)
                if (colNames.get(i).equals(yCol))
                    yColId = i;
        }
        return yColId;
    }

    public ArrayList<String> getColumnNames() {

        // if it is specified already, return
        if (columnNames.size() > 0)
            return columnNames;

        // otherwise fetch the schema from DB
        if (queriedColumnNames == null)
            try {
                queriedColumnNames = new ArrayList<>();
                Statement rawDBStmt = DbConnector.getStmtByDbName(this.getDb());
                ResultSet rs = DbConnector.getQueryResultIterator(rawDBStmt, this.getQuery());
                int colCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= colCount; i ++)
                    queriedColumnNames.add(rs.getMetaData().getColumnName(i));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        return queriedColumnNames;
    }

    public int getNumLevels() {
        return numLevels;
    }

    public int getTopLevelWidth() {
        return topLevelWidth;
    }

    public int getTopLevelHeight() {
        return topLevelHeight;
    }

    public int getZoomFactor() {
        return zoomFactor;
    }

    // get the canvas coordinate of a raw value
    public double getCanvasCoordinate(int level, double v, boolean isX) {

        // calculate range if have not
        if (Double.isNaN(loX)) {

            System.out.println("\n Calculating autoDD x & y ranges...\n");
            loX = loY = Double.MAX_VALUE;
            hiX = hiY = Double.MIN_VALUE;
            try {
                Statement rawDBStmt = DbConnector.getStmtByDbName(this.getDb());
                ResultSet rs = DbConnector.getQueryResultIterator(rawDBStmt, this.getQuery());
                while (rs.next()) {
                    double cx = rs.getDouble(this.getXColId() + 1);
                    double cy = rs.getDouble(this.getYColId() + 1);
                    loX = Math.min(loX, cx); hiX = Math.max(hiX, cx);
                    loY = Math.min(loY, cy); hiY = Math.max(hiY, cy);
                }
                rawDBStmt.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        if (isX)
            return (topLevelWidth - bboxW) * (v - loX) / (hiX - loX) * Math.pow(zoomFactor, level) + bboxW / 2.0;
        else
            return (topLevelHeight - bboxH) * (v - loY) / (hiY - loY) * Math.pow(zoomFactor, level) + bboxH / 2.0;
    }

    @Override
    public String toString() {
        return "AutoDD{" +
                "query='" + query + '\'' +
                ", db='" + db + '\'' +
                ", xCol='" + xCol + '\'' +
                ", yCol='" + yCol + '\'' +
                ", bboxW=" + bboxW +
                ", bboxH=" + bboxH +
                ", rendering='" + rendering + '\'' +
                ", columnNames=" + columnNames +
                ", numLevels=" + numLevels +
                ", topLevelWidth=" + topLevelWidth +
                ", topLevelHeight=" + topLevelHeight +
                ", zoomFactor=" + zoomFactor +
                '}';
    }
}