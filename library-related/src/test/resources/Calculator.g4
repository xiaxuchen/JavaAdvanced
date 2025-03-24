grammar Calculator;

line : expr EOF ;
expr : '(' expr ')' # parenExpr
    | expr op=('*'|'/') expr # mulDiv
    | expr op=('+'|'-') expr # addSub
    | FLOAT # float
;
 WS : [ \t\r\n]+ -> skip ;
 FLOAT : DIGIT+ '.' DIGIT* EXPONENT?
    | '.' DIGIT+ EXPONENT?
    | DIGIT+ EXPONENT? ;

fragment DIGIT : [0-9] ;
fragment EXPONENT : [eE] [+\-]? DIGIT+ ;