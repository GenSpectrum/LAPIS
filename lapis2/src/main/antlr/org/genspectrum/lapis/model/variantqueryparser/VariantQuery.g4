grammar VariantQuery;

// parser rules

start: expr EOF;
expr:
  single             # Uni
  | '!' expr         # Not
  | expr '&' expr    # And
  | expr '|' expr    # Or
  | '(' expr ')'     # Parentesis
  | 'MAYBE(' expr ')' # Maybe
  ;

single:
  nucleotide_mutation
  ;

nucleotide_mutation : nucleotide_symbol? position ambigous_nucleotide_symbol?;

position: NUMBER+;
nucleotide_symbol: A | C | G | T;
ambigous_nucleotide_symbol: nucleotide_symbol | M | R | W | S | Y | K | V | H | D | B | N | MINUS | DOT;

// lexer rules

A: 'A';
B: 'B';
C: 'C';
D: 'D';
E: 'E';
F: 'F';
G: 'G';
H: 'H';
I: 'I';
J: 'J';
K: 'K';
L: 'L';
M: 'M';
N: 'N';
O: 'O';
P: 'P';
Q: 'Q';
R: 'R';
S: 'S';
T: 'T';
U: 'U';
V: 'V';
W: 'W';
X: 'X';
Y: 'Y';
Z: 'Z';
MINUS: '-';
DOT: '.';

NUMBER: [0-9];
WHITESPACE: [ \r\n\t] -> skip;
