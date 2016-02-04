/*:- multifile propagates/2.
:- multifile has_policy/2.
:- multifile has_relation/2.
*/
/* rdfs:subPropertyOf */
 /*rdfs_sub_property_of(a,b) . This is just to allow the rule not*/
i_rdfs_sub_property_of(X,X) .
i_rdfs_sub_property_of(X,Y) :- 
  rdfs_sub_property_of(X,Z),i_rdfs_sub_property_of(Z,Y) .

/* owl:inverseof 
i_owl_inverse_of(A,B) :- owl_inverse_of(A,B) .
i_owl_inverse_of(B,A) :- i_owl_inverse_of(A,B) .
i_has_relation(S,T,R) :- has_relation(S,T,R) .
i_has_relation(T,S,I) :- i_has_relation(S,T,R) , i_owl_inverse_of(R,I) .
*/
/*i_has_relation(S,T,R) :- has_relation(S,T,R) .
i_has_relation(S,T,R) :- has_relation(S,T,Q) , i_rdfs_sub_property_of(Q,R) . */

/*i_has_relation(T,S,I) :- i_has_relation(S,T,R) , i_owl_inverse_of(R,I) .*/

/* PPR */
i_propagates(X,Y) :- propagates(X,Y) .
i_propagates(X,Y) :- propagates(Z,Y) , i_rdfs_sub_property_of(X,Z).
i_has_policy(T,P) :- has_policy(T,P) .
i_has_policy(T,P) :- has_relation(S,T,R) , i_propagates(R,P) , i_has_policy(S,P).
/*
i_connected(N,N) .
i_connected(N1,N2) :- has_relation(N1,N2,Z) .
i_connected(N1,N3) :- has_relation(N2,N3,Z) , i_connected(N1,N2) . */
