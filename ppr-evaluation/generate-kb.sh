#!/bin/bash

TARGET="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TARGET=$TARGET/kb2
DIR=$TARGET/../dn-propag/


#ppm export ontology rdf --changes=0 > $TARGET/datanode-c0.nt
#ppm export ontology rdf > $TARGET/datanode-cALL.nt

ppm export ontology rdf --compact --changes=0 > $TARGET/datanode-compact-c0.nt
ppm export ontology rdf --compact > $TARGET/datanode-compact-cALL.nt
ppm export ontology prolog --compact --changes=0 > $TARGET/datanode-compact-c0.pl
ppm export ontology prolog --compact > $TARGET/datanode-compact-cALL.pl

ppm export ontology rdf --compact --nohierarchy --changes=0 > $TARGET/datanode-no-hierarchy-c0.nt
ppm export ontology rdf --compact --nohierarchy > $TARGET/datanode-no-hierarchy-cALL.nt
ppm export ontology prolog --compact --nohierarchy --changes=0 > $TARGET/datanode-no-hierarchy-c0.pl
ppm export ontology prolog --compact --nohierarchy > $TARGET/datanode-no-hierarchy-cALL.pl

ppm export rules rdf --changes=0 > $TARGET/full-rules-c0.nt
ppm export rules rdf > $TARGET/full-rules-cALL.nt
ppm export rules prolog --changes=0 > $TARGET/full-rules-c0.pl
ppm export rules prolog > $TARGET/full-rules-cALL.pl

ppm export rules rdf --compress --changes=0 >  $TARGET/compressed-rules-c0.nt
ppm export rules rdf --compress > $TARGET/compressed-rules-cALL.nt
ppm export rules prolog --compress --changes=0 > $TARGET/compressed-rules-c0.pl
ppm export rules prolog --compress > $TARGET/compressed-rules-cALL.pl


