#!/bin/bash
for file in /data/dspace/temp/*; do
echo /data/dspace/bin/dspace packager -s -t AIP -e dm-admin@statsbiblioteket.dk -p 1902/186 $file;
done