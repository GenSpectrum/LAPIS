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
  | pangolineage_query
  | n_of_query
  ;

nucleotide_mutation : nucleotide_symbol? position ambigous_nucleotide_symbol?;
position: NUMBER+;
nucleotide_symbol: A | C | G | T;
ambigous_nucleotide_symbol: nucleotide_symbol | M | R | W | S | Y | K | V | H | D | B | N | MINUS | DOT;

pangolineage_query: pangolineage pangolineage_include_sublineages?;
pangolineage: pangolineage_character pangolineage_character? pangolineage_character? pangolineage_number_component*;
pangolineage_character: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z;
pangolineage_number_component: '.' NUMBER NUMBER? NUMBER?;
pangolineage_include_sublineages: DOT? ASTERISK;

n_of_query: '[' n_of_match_exactly? n_of_number_of_matchers '-of:' n_of_exprs ']';
n_of_match_exactly: 'EXACTLY-';
n_of_number_of_matchers: NUMBER+;
n_of_exprs: expr (',' expr)*;

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
ASTERISK: '*';

NUMBER: [0-9];
WHITESPACE: [ \r\n\t] -> skip;
