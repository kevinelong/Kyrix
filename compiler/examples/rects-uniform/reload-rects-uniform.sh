#!/bin/bash
# wrapped as a script for start-kyrix.sh

if [ "x$PGCONN" = "x" ]; then echo "$0: PGCONN must be set."; exit 1; fi
if [ "x$PSQL" ]; then PSQL=`which psql`; fi
if [ ! -x $PSQL ]; then echo "$0: $PSQL not found - consider setting PSQL to the psql(1) path."; exit 1; fi
SCALE=${SCALE:-1}  # times 1M records
echo "PGCONN:";
echo $PGCONN;

$PSQL $PGCONN -t -c "drop table if exists rects_uniform cascade; create table rects_uniform(id int, x int, y int,  w int, h int, citus_distribution_id int);"
"
done
for i in {1..100}; do
    echo `date +%s`": loading rects_uniform data #$i of 100..."
    $PSQL $PGCONN -q -t -c "insert into rects_uniform (id,x,y,w,h, citus_distribution_id) select id, (random()*100000)::bigint, (random()*10000)::bigint, (random()*10000)::bigint, (random()*10000)::bigint, (random()*2147483648*2-2147483648)::int from generate_series(1,10000*$SCALE) id;
