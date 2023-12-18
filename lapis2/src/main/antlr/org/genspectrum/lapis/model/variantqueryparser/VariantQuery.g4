grammar VariantQuery;

// parser rules

start: expr EOF;
expr:
  single             # Uni
  | '!' expr         # Not
  | expr '&' expr    # And
  | expr '|' expr    # Or
  | '(' expr ')'     # Parenthesis
  | 'MAYBE(' expr ')' # Maybe
  ;

single:
  nucleotideMutationQuery
  | pangolineageQuery
  | nOfQuery
  | nucleotideInsertionQuery
  | aaMutationQuery
  | aaInsertionQuery
  | nextcladePangolineageQuery
  | nextstrainCladeLineageQuery
  | gisaidCladeLineageQuery
  ;

nucleotideMutationQuery : nucleotideMutationQueryFirstSymbol? position nucleotideMutationQuerySecondSymbol?;
nucleotideMutationQueryFirstSymbol: nucleotideSymbol;
nucleotideMutationQuerySecondSymbol: possibleAmbiguousNucleotideSymbol;
position: NUMBER+;
nucleotideSymbol: A | C | G | T;
ambiguousNucleotideSymbol: M | R | W | S | Y | K | V | H | D | B | N | MINUS | DOT;
possibleAmbiguousNucleotideSymbol: nucleotideSymbol | ambiguousNucleotideSymbol;


pangolineageQuery: pangolineage pangolineageIncludeSublineages?;
pangolineage: pangolineageCharacter pangolineageCharacter? pangolineageCharacter? pangolineageNumberComponent*;
pangolineageCharacter: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z;
pangolineageNumberComponent: '.' NUMBER NUMBER? NUMBER?;
pangolineageIncludeSublineages: DOT? ASTERISK;

nOfQuery: '[' nOfMatchExactly? nOfNumberOfMatchers nOfOfKeyword nOfExprs ']';
nOfOfKeyword: '-of:' | '-OF:';
nOfMatchExactly: 'EXACTLY-' | 'exactly-';
nOfNumberOfMatchers: NUMBER+;
nOfExprs: expr (',' expr)*;

nucleotideInsertionQuery: insertionKeyword position ':' nucleotideInsertionSymbol+;
nucleotideInsertionSymbol: possibleAmbiguousNucleotideSymbol | '?';
insertionKeyword: 'ins_' | 'INS_';

aaMutationQuery: gene ':' aaSymbol? position possiblyAmbiguousAaSymbol?;
aaSymbol: A | R | N | D | C | E | Q | G | H | I | L | K | M | F | P | S | T | W | Y | V | ASTERISK;
ambiguousAaSymbol: X | MINUS | DOT;
possiblyAmbiguousAaSymbol: aaSymbol | ambiguousAaSymbol;
gene: covidGene;
covidGene : E | M | N | S | ORF;

aaInsertionQuery: insertionKeyword gene ':' position ':' aaInsertionSymbol+;
aaInsertionSymbol: possiblyAmbiguousAaSymbol | '?';

nextcladePangolineageQuery: nextcladePangoLineagePrefix pangolineageQuery;
nextcladePangoLineagePrefix: 'nextcladePangoLineage:' | 'NEXTCLADEPANGOLINEAGE:' | 'nextcladepangolineage:';

nextstrainCladeLineageQuery: nextstrainCladePrefix nextstrainCladeQuery;
nextstrainCladePrefix: 'nextstrainClade:'| 'NEXTSTRAINCLADE:' | 'nextstrainclade:';
nextstrainCladeQuery: NUMBER NUMBER nextstrainCladeCharacter | 'RECOMBINANT' | 'recombinant';
nextstrainCladeCharacter: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z;

gisaidCladeLineageQuery: gisaidCladePrefix gisaidCladeNomenclature;
gisaidCladePrefix: ('gisaid:'| 'GISAID:');
gisaidCladeNomenclature: gisaid_clade_character gisaid_clade_character?;
gisaid_clade_character: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z;


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
DOT: '.';
ASTERISK: '*';

NUMBER: [0-9];
WHITESPACE: [ \r\n\t] -> skip;

ORF1A: 'ORF1A' | 'orf1a';
ORF1B: 'ORF1B' | 'orf1b';
ORF3A: 'ORF3A' | 'orf3a';
ORF6: 'ORF6' | 'orf6';
ORF7A: 'ORF7A' | 'orf7a';
ORF7B: 'ORF7B' | 'orf7b';
ORF8: 'ORF8' | 'orf8';
ORF9B: 'ORF9B' | 'orf9b';

ORF: ORF1A | ORF1B | ORF3A | ORF6 | ORF7A | ORF7B | ORF8 | ORF9B;
