#!/bin/bash

TARGET="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TARGET=$TARGET/kb1
DIR=$TARGET/../dn-propag/


ppr export ontology rdf --changes=0 > $TARGET/datanode-c0.nt
ppr export ontology rdf > $TARGET/datanode-cALL.nt
ppr export relations rdf --changes=0 > $TARGET/flat-relations-c0.nt
ppr export relations rdf > $TARGET/flat-relations-cALL.nt

ppr export ontology rdf --compact --changes=0 > $TARGET/relations-c0.nt
ppr export ontology rdf --compact > $TARGET/relations-cALL.nt
ppr export relations prolog --changes=0 > $TARGET/relations-c0.pl
ppr export relations prolog > $TARGET/relations-cALL.pl

ppr export rules rdf --changes=0 > $TARGET/full-rules-c0.nt
ppr export rules rdf > $TARGET/full-rules-cALL.nt
ppr export rules prolog --changes=0 > $TARGET/full-rules-c0.pl
ppr export rules prolog > $TARGET/full-rules-cALL.pl

ppr export rules rdf --compress --changes=0 >  $TARGET/compressed-rules-c0.nt
ppr export rules rdf --compress > $TARGET/compressed-rules-cALL.nt
ppr export rules prolog --compress --changes=0 > $TARGET/compressed-rules-c0.pl
ppr export rules prolog --compress > $TARGET/compressed-rules-cALL.pl


