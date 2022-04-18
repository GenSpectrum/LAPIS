/**
 * Examples of allowed queries:
 *   - B.1.1.7  (pango lineage)
 *   - P.1* | (S:484K & B.1.1.7)  ("*" means that sub-lineages are included)
 *   - P.1* | S:484K & B.1.1.7  (AND takes precedence over OR, i.e., this is equivalent to the query above)
 *   - NEXTSTRAIN:21K | GISAID:GR  ("NEXTSTRAIN:" -> Nextstrain clade, "GISAID:" -> GISAID clade)
 *   - S:N501  (The spike gene has a mutation at position 501; a deletion is considered as a mutation)
 *   - !123- & 123  (The nucleotide at position 123 is not deleted, but it is mutated)
 *   - [2-OF: S:N501Y, P.1* & 123-, [EXACTLY-2-OF: 123, 234T, NEXTSTRAIN:21K]]
*/
grammar VariantQuery;
@header {
    package ch.ethz.lapis.api.parser;
}


// parser rules

start: expr EOF;
expr:
  single             # Uni
  | '!' expr         # Neg
  | expr '&' expr    # And
  | expr '|' expr    # Or
  | '(' expr ')'     # Par
  ;
single: aa_mut | nuc_mut | pango_query | gisaid_clade_query | nextstrain_clade_query | n_of;

nuc_mut : nuc? position nuc_mutated?;
aa_mut : gene ':' aa? position aa_mutated?;

position: NUMBER+;
aa: A | R | N | D | C | E | Q | G | H | I | L | K | M | F | P | S | T | W | Y | V | ASTERISK;
aa_mutated: aa | X | MINUS;
nuc: A | C | G | T;
nuc_mutated: nuc | M | R | W | S | Y | K | V | H | D | B | N | MINUS;
gene: E | M | N | S | ORF;

pango_query: pango_lineage pango_include_sub?;
pango_include_sub: DOT? ASTERISK;
// We accept inputs like B.1.617.2.1 which we would translate to AY.1. This is unofficial, but useful.
pango_lineage: character character? pango_number_component*;
pango_number_component: '.' NUMBER NUMBER? NUMBER?;

gisaid_clade: character character?;
gisaid_clade_prefix: G I S A I D ':';
gisaid_clade_query: gisaid_clade_prefix gisaid_clade;
nextstrain_clade: NUMBER NUMBER character;
nextstrain_clade_prefix: N E X T S T R A I N ':';
nextstrain_clade_query: nextstrain_clade_prefix nextstrain_clade;

character: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z;

n_of: '[' n_of_exactly? n_of_n '-OF:' n_of_exprs ']';
n_of_exactly: 'EXACTLY-';
n_of_n: NUMBER+;
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
ORF: 'ORF1A' | 'ORF1B' | 'ORF3A' | 'ORF6' | 'ORF7A' | 'ORF7B' | 'ORF8' | 'ORF9b';

NUMBER: [0-9];
WHITESPACE: [ \r\n\t] -> skip;
