grammar AdvancedQuery;

// parser rules

start: expr EOF;
expr:
  both             # Uni
  | not_ expr         # Not
  | expr and_ expr    # And
  | expr or_ expr    # Or
  | '(' expr ')'     # Parenthesis
  | maybeQuery       # Maybe
  ;

both: 
  single
  | metadataQueryExpr
  ;

single:
  singleSegmentedMutationQuery
  | nOfQuery
  | nucleotideInsertionQuery
  | namedMutationQuery
  | namedInsertionQuery
  ;

metadataQueryExpr:
  metadataQuery
  | metadataGreaterThanEqualQuery
  | metadataLessThanEqualQuery
  | isNullQuery
  ;

variantExpr:
  single # VariantUni
  | not_ variantExpr # VariantNot
  | variantExpr and_ variantExpr # VariantAnd
  | variantExpr or_ variantExpr # VariantOr
  | '(' variantExpr ')'  # VariantParenthesis
  | maybe_ '(' variantExpr ')' # VariantMaybe
  ;

// Maybe queries should only be used in the context of variant queries
maybeQuery:
  maybe_ '(' variantExpr ')'
  ;

or_: OR | '|';
maybe_: M A Y B E;
not_: NOT | '!';
and_: AND | '&';
position: NUMBER;

singleSegmentedMutationQuery : singleSegmentedMutationQueryFirstSymbol? position singleSegmentedMutationQuerySecondSymbol?;
singleSegmentedMutationQueryFirstSymbol: nucleotideSymbol;
singleSegmentedMutationQuerySecondSymbol: possibleAmbiguousNucleotideSymbol;
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

namedMutationQuery: name ':' mutationQueryFirstSymbol? position mutationQuerySecondSymbol?;
mutationQueryFirstSymbol: nucleotideSymbol | aaSymbol;
mutationQuerySecondSymbol: nucleotideSymbol | ambiguousNucleotideSymbol | aaSymbol | ambiguousAaSymbol;
aaSymbol: A | R | N | D | C | E | Q | G | H | I | L | K | M | F | P | S | T | W | Y | V | ASTERISK;
ambiguousAaSymbol: X | MINUS | DOT;
possiblyAmbiguousAaSymbol: aaSymbol | ambiguousAaSymbol;

namedInsertionQuery: insertionKeyword name ':' position ':' namedInsertionSymbol+;
namedInsertionSymbol: possibleAmbiguousNucleotideSymbol | possiblyAmbiguousAaSymbol | '?';

metadataGreaterThanEqualQuery: name '>=' name;
metadataLessThanEqualQuery: name '<=' name;
metadataQuery: name '=' value;
value: name | QUOTED_STRING;

dateOrNumber: digit+;
digit: NUMBER | MINUS | DOT;
name: charOrNumber+;
charOrNumber: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z | NUMBER | MINUS | UNDERSCORE | DOT | ASTERISK;

isNullQuery: isnull_ '(' name ')';
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
QUOTED_STRING: '\'' (~['\r\n])* '\'';  // matches all strings with quotes, except if they contain a newline
AND: ' ' A N D ' '; // space is important here, otherwise metadataNames with 'AND' in them would be misinterpreted
OR: ' ' O R ' ';
NOT: N O T ' ';

NUMBER: [0-9]+;
WHITESPACE: [ \r\n\t] -> skip;
