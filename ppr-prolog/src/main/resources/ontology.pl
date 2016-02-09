/*:- multifile propagates/2.
:- multifile has_policy/2.
:- multifile has_relation/2.
*/
/* rdfs:subPropertyOf */
 /*rdfs_subproperty_of(a,b) . This is just to allow the rule not*/
i_rdfs_sub_property_of(X,X) . 
i_rdfs_sub_property_of(X,Y) :- 
  rdfs_sub_property_of(X,Z), i_rdfs_sub_property_of(Z,Y) .

/*owl_inverse_of(x______,y_____).*/
/* owl:inverseof */
i_owl_inverse_of(A,B) :- owl_inverse_of(A,B).
i_owl_inverse_of(A,B) :- owl_inverse_of(B,A).
i_has_relation(S,T,R,_) :- has_relation(S,T,R) .
i_has_relation(T,S,I,L) :- i_owl_inverse_of(I,R), not(visited(R,L)), i_has_relation(S,T,R,[R|L]) . 
/*i_has_relation(S,T,R,L) :- not(visited(Z,L)), i_has_relation(S,T,Z,[Z|L]) . */
i_has_relation(S,T,R)   :- i_has_relation(S,T,R,[R]). 

/* List Membership */
visited(X,[Y|_]) :- (X=Y) .
visited(X,[Y|R]) :- not(X=Y) , visited(X,R) .

/* PPR */
i_propagates(X,Y) :- propagates(X,Y) .
i_propagates(X,Y) :- i_rdfs_sub_property_of(X,Z), propagates(Z,Y) .
/*i_has_policy(T,P,[]) :- has_policy(T,P) .*/
/*i_has_policy(T,P,[]) :- has_policy(T,P) .*/
i_has_policy(T,P,_) :- has_policy(T,P) . 
i_has_policy(T,P,L) :- i_has_relation(S,T,R), not(visited(S,L)), i_propagates(R,P), i_has_policy(S,P,[S|L]) .
/*i_has_policy(T,P,L) :- not(visited(T,L)), i_has_policy(S,P,[T|L]), i_propagates(R,P), i_has_relation(S,T,R) .*/
/*i_has_policy(T,P,L) :- i_has_policy(S,P),i_propagates(R,P),i_has_relation(S,T,R) . */
i_has_policy(T,P) :- i_has_policy(T,P,[]) . 

/*
i_connected(N,N) .
i_connected(N1,N2) :- has_relation(N1,N2,_) .
i_connected(N1,N3) :- has_relation(N2,N3,_) , i_connected(N1,N2) .
*/