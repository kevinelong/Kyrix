#!/bin/sh
# wrapped as a script for start-kyrix.sh

cd /kyrix/compiler/examples/rects-uniform
node rects.js | egrep -i "error|connected" || true
