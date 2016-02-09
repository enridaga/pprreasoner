:- consult(full-rules).
:- consult(relations).
:- consult(ontology).

/* Test for the attribution */
has_policy(source1,'http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#Attribution') .
has_policy(source1,'http://www.w3.org/ns/odrl/2/prohibition http://www.w3.org/ns/odrl/2/sell') .
has_relation(source1,output,'http://purl.org/datanode/ns/hasPortion') .


has_policy(source2,'http://www.w3.org/ns/odrl/2/prohibition http://www.w3.org/ns/odrl/2/attachPolicy') .
has_policy(source2,'http://www.w3.org/ns/odrl/2/duty http://creativecommons.org/ns#Attribution') .
has_relation(source2,tmp5,'http://purl.org/datanode/ns/hasCopy') .
/*has_relation(tmp1,tmp2,dn_hasCopy) .
has_relation(tmp1,tmp2,dn_refactoredInto) .
has_relation(tmp2,tmp3,dn_remodelledTo) .
has_relation(tmp3,tmp4,dn_hasSelection) .
has_relation(tmp4,tmp5,dn_hasSelection) .*/
has_relation(tmp5,output2, 'http://purl.org/datanode/ns/hasSelection') .
/*has_relation(output2,tmp5,'http://purl.org/datanode/ns/remodelledFrom') .
has_relation(tmp5,output2,'http://purl.org/datanode/ns/remodelledTo') .*/

/*
has_policy(source1,duty_cc_Attribution) .
has_relation(source1,tmp1,dn_hasPart) .
has_relation(tmp2,tmp1,dn_isPartOf) .
has_relation(output,tmp2,dn_isSelectionOf) .
*/

