grammar AdvancedQuery;

// parser rules

start: expr EOF;
expr:
  single             # Uni
  | not_ expr         # Not
  | expr and_ expr    # And
  | expr or_ expr    # Or
  | '(' expr ')'     # Parenthesis
  | maybe_ '(' expr ')' # Maybe
  ;

single:
  nucleotideMutationQuery
  | nOfQuery
  | nucleotideInsertionQuery
  | aaMutationQuery
  | aaInsertionQuery
  | metadataQuery
  | isNullQuery
  ;

or_: OR | '|';
maybe_: M A Y B E;
not_: NOT | '!';
and_: AND | '&';
position: NUMBER+;

nucleotideMutationQuery : nucleotideMutationQueryFirstSymbol? position nucleotideMutationQuerySecondSymbol?;
nucleotideMutationQueryFirstSymbol: nucleotideSymbol;
nucleotideMutationQuerySecondSymbol: possibleAmbiguousNucleotideSymbol;
nucleotideSymbol: A | C | G | T;
ambiguousNucleotideSymbol: M | R | W | S | Y | K | V | H | D | B | N | MINUS | DOT;
possibleAmbiguousNucleotideSymbol: nucleotideSymbol | ambiguousNucleotideSymbol;

nOfQuery: '[' nOfMatchExactly? nOfNumberOfMatchers nOfOfKeyword nOfExprs ']';
nOfOfKeyword: '-' O F ':';
nOfMatchExactly: E X A C T L Y '-';
nOfNumberOfMatchers: NUMBER+;
nOfExprs: expr (',' expr)*;

nucleotideInsertionQuery: insertionKeyword position ':' nucleotideInsertionSymbol+;
nucleotideInsertionSymbol: possibleAmbiguousNucleotideSymbol | '?';
insertionKeyword: I N S '_';

aaMutationQuery: geneOrName ':' aaSymbol? position possiblyAmbiguousAaSymbol?;
aaSymbol: A | R | N | D | C | E | Q | G | H | I | L | K | M | F | P | S | T | W | Y | V | ASTERISK;
ambiguousAaSymbol: X | MINUS | DOT;
possiblyAmbiguousAaSymbol: aaSymbol | ambiguousAaSymbol;
aaInsertionQuery: insertionKeyword geneOrName ':' position ':' aaInsertionSymbol+;
aaInsertionSymbol: possiblyAmbiguousAaSymbol | '?';

metadataQuery: geneOrName '=' value;
value: geneOrName | STRING;
metadataGreaterThanEqualQuery: geneOrName '>=' dateOrNumber;
metadataLessThanEqualQuery: geneOrName '<=' dateOrNumber;

geneOrName: charOrNumber+;
charOrNumber: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z | NUMBER | MINUS | UNDERSCORE | DOT;
dateOrNumber: NUMBER | MINUS | DOT;

isNullQuery: isnull_ '(' geneOrName ')';
isnull_: I S N U L L ;


// lexer rules
A: 'A' | 'a';
B: 'B' | 'b';
C: 'C' | 'c';
D: 'D' | 'd';
E: 'E' | 'e';
F: 'F' | 'f';
G: 'G' | 'g';
H: 'H' | 'h';
I: 'I' | 'i';
J: 'J' | 'j';
K: 'K' | 'k';
L: 'L' | 'l';
M: 'M' | 'm';
N: 'N' | 'n';
O: 'O' | 'o';
P: 'P' | 'p';
Q: 'Q' | 'q';
R: 'R' | 'r';
S: 'S' | 's';
T: 'T' | 't';
U: 'U' | 'u';
V: 'V' | 'v';
W: 'W' | 'w';
X: 'X' | 'x';
Y: 'Y' | 'y';
Z: 'Z' | 'z';
MINUS: '-';
UNDERSCORE: '_';
DOT: '.';
ASTERISK: '*';
STRING: '\'' (~['\r\n])* '\'';
AND: ' AND '; // space is important here, otherwise metadataNames with 'AND' in them would be misinterpreted
OR: ' OR ';
NOT: 'NOT ';

NUMBER: [0-9];
WHITESPACE: [ \r\n\t] -> skip;
