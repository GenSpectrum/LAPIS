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
  nucleotide_mutation_query
  | pangolineage_query
  | n_of_query
  | nucleotide_insertion_query
  | aa_mutation_query
  | aa_insertion_query
  | nextclade_pangolineage_query
  | nextstrain_clade_lineage_query
  | gisaid_clade_lineage_query
  ;

nucleotide_mutation_query : nucleotide_mutation_query_first_symbol? position nucleotide_mutation_query_second_symbol?;
nucleotide_mutation_query_first_symbol: nucleotide_symbol;
nucleotide_mutation_query_second_symbol: possible_ambigous_nucleotide_symbol;
position: NUMBER+;
nucleotide_symbol: A | C | G | T;
ambigous_nucleotide_symbol: M | R | W | S | Y | K | V | H | D | B | N | MINUS | DOT;
possible_ambigous_nucleotide_symbol: nucleotide_symbol | ambigous_nucleotide_symbol;


pangolineage_query: pangolineage pangolineage_include_sublineages?;
pangolineage: pangolineage_character pangolineage_character? pangolineage_character? pangolineage_number_component*;
pangolineage_character: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z;
pangolineage_number_component: '.' NUMBER NUMBER? NUMBER?;
pangolineage_include_sublineages: DOT? ASTERISK;

n_of_query: '[' n_of_match_exactly? n_of_number_of_matchers no_of_of_keyword n_of_exprs ']';
no_of_of_keyword: '-of:' | '-OF:';
n_of_match_exactly: 'EXACTLY-' | 'exactly-';
n_of_number_of_matchers: NUMBER+;
n_of_exprs: expr (',' expr)*;

nucleotide_insertion_query: insertion_keyword position ':' (possible_ambigous_nucleotide_symbol | '?')+;
insertion_keyword: 'ins_' | 'INS_';

aa_mutation_query: gene ':' aa_symbol? position possible_ambigous_aa_symbol?;
aa_symbol: A | R | N | D | C | E | Q | G | H | I | L | K | M | F | P | S | T | W | Y | V | ASTERISK;
ambigous_aa_symbol: X | MINUS | DOT;
possible_ambigous_aa_symbol: aa_symbol | ambigous_aa_symbol;
gene: covid_gene;
covid_gene : E | M | N | S | ORF;

aa_insertion_query: insertion_keyword gene ':' position ':' (possible_ambigous_aa_symbol | '?')+;

nextclade_pangolineage_query: nextclade_pango_lineage_prefix pangolineage_query;
nextclade_pango_lineage_prefix: 'nextcladePangoLineage:' | 'NEXTCLADEPANGOLINEAGE:';

nextstrain_clade_lineage_query: nextstrain_clade_prefix nextstrain_clade_query;
nextstrain_clade_prefix: 'nextstrainClade:'| 'NEXTSTRAINCLADE:';
nextstrain_clade_query: NUMBER NUMBER nextstrain_clade_character | 'RECOMBINANT';
nextstrain_clade_character: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z;

gisaid_clade_lineage_query: gisaid_clade_prefix gisaid_clade_query;
gisaid_clade_prefix: ('gisaid:'| 'GISAID:');
gisaid_clade_query: gisaid_clade_character gisaid_clade_character?;
gisaid_clade_character: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z;


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

ORF: 'ORF1A' | 'ORF1B' | 'ORF3A' | 'ORF6' | 'ORF7A' | 'ORF7B' | 'ORF8' | 'ORF9B';
