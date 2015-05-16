#!/bin/sh

INDEX_HTML="$(dirname "$0")/javadoc/index.html"
xdg-open "$INDEX_HTML" || open "$INDEX_HTML"
