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

nucleotideInsertionQuery: insertionKeyword position ':' (possibleAmbiguousNucleotideSymbol | '?')+;
insertionKeyword: 'ins_' | 'INS_';

aaMutationQuery: gene ':' aaSymbol? position possibleAmbiguousAaSymbol?;
aaSymbol: A | R | N | D | C | E | Q | G | H | I | L | K | M | F | P | S | T | W | Y | V | ASTERISK;
ambiguousAaSymbol: X | MINUS | DOT;
possibleAmbiguousAaSymbol: aaSymbol | ambiguousAaSymbol;
gene: covidGene;
covidGene : E | M | N | S | ORF;

aaInsertionQuery: insertionKeyword gene ':' position ':' (possibleAmbiguousAaSymbol | '?')+;

nextcladePangolineageQuery: nextcladePangoLineagePrefix pangolineageQuery;
nextcladePangoLineagePrefix: 'nextcladePangoLineage:' | 'NEXTCLADEPANGOLINEAGE:';

nextstrainCladeLineageQuery: nextstrainCladePrefix nextstrainCladeQuery;
nextstrainCladePrefix: 'nextstrainClade:'| 'NEXTSTRAINCLADE:';
nextstrainCladeQuery: NUMBER NUMBER nextstrainCladeCharacter | 'RECOMBINANT';
nextstrainCladeCharacter: A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z;

gisaidCladeLineageQuery: gisaidCladePrefix gisaidCladeNomenclature;
gisaidCladePrefix: ('gisaid:'| 'GISAID:');
gisaidCladeNomenclature: gisaid_clade_character gisaid_clade_character?;
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
