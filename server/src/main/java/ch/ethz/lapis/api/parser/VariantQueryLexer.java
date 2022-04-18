// Generated from ch\ethz\lapis\api\parser\VariantQuery.g4 by ANTLR 4.9.3

    package ch.ethz.lapis.api.parser;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class VariantQueryLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9,
		T__9=10, T__10=11, A=12, B=13, C=14, D=15, E=16, F=17, G=18, H=19, I=20,
		J=21, K=22, L=23, M=24, N=25, O=26, P=27, Q=28, R=29, S=30, T=31, U=32,
		V=33, W=34, X=35, Y=36, Z=37, MINUS=38, DOT=39, ASTERISK=40, ORF=41, NUMBER=42,
		WHITESPACE=43;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8",
			"T__9", "T__10", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
			"L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y",
			"Z", "MINUS", "DOT", "ASTERISK", "ORF", "NUMBER", "WHITESPACE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'!'", "'&'", "'|'", "'('", "')'", "':'", "'['", "'-OF:'", "']'",
			"'EXACTLY-'", "','", "'A'", "'B'", "'C'", "'D'", "'E'", "'F'", "'G'",
			"'H'", "'I'", "'J'", "'K'", "'L'", "'M'", "'N'", "'O'", "'P'", "'Q'",
			"'R'", "'S'", "'T'", "'U'", "'V'", "'W'", "'X'", "'Y'", "'Z'", "'-'",
			"'.'", "'*'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null,
			"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
			"O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "MINUS",
			"DOT", "ASTERISK", "ORF", "NUMBER", "WHITESPACE"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public VariantQueryLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "VariantQuery.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2-\u00e1\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3"+
		"\t\3\t\3\t\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\f\3"+
		"\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23"+
		"\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\32\3\32"+
		"\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\""+
		"\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'\3(\3(\3)\3)\3*\3*\3*\3*\3*\3*\3*\3*\3"+
		"*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3"+
		"*\3*\3*\3*\3*\3*\3*\5*\u00da\n*\3+\3+\3,\3,\3,\3,\2\2-\3\3\5\4\7\5\t\6"+
		"\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24"+
		"\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K"+
		"\'M(O)Q*S+U,W-\3\2\4\3\2\62;\5\2\13\f\17\17\"\"\2\u00e7\2\3\3\2\2\2\2"+
		"\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2"+
		"\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2"+
		"\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2"+
		"\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2"+
		"\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2"+
		"\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2"+
		"K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3"+
		"\2\2\2\3Y\3\2\2\2\5[\3\2\2\2\7]\3\2\2\2\t_\3\2\2\2\13a\3\2\2\2\rc\3\2"+
		"\2\2\17e\3\2\2\2\21g\3\2\2\2\23l\3\2\2\2\25n\3\2\2\2\27w\3\2\2\2\31y\3"+
		"\2\2\2\33{\3\2\2\2\35}\3\2\2\2\37\177\3\2\2\2!\u0081\3\2\2\2#\u0083\3"+
		"\2\2\2%\u0085\3\2\2\2\'\u0087\3\2\2\2)\u0089\3\2\2\2+\u008b\3\2\2\2-\u008d"+
		"\3\2\2\2/\u008f\3\2\2\2\61\u0091\3\2\2\2\63\u0093\3\2\2\2\65\u0095\3\2"+
		"\2\2\67\u0097\3\2\2\29\u0099\3\2\2\2;\u009b\3\2\2\2=\u009d\3\2\2\2?\u009f"+
		"\3\2\2\2A\u00a1\3\2\2\2C\u00a3\3\2\2\2E\u00a5\3\2\2\2G\u00a7\3\2\2\2I"+
		"\u00a9\3\2\2\2K\u00ab\3\2\2\2M\u00ad\3\2\2\2O\u00af\3\2\2\2Q\u00b1\3\2"+
		"\2\2S\u00d9\3\2\2\2U\u00db\3\2\2\2W\u00dd\3\2\2\2YZ\7#\2\2Z\4\3\2\2\2"+
		"[\\\7(\2\2\\\6\3\2\2\2]^\7~\2\2^\b\3\2\2\2_`\7*\2\2`\n\3\2\2\2ab\7+\2"+
		"\2b\f\3\2\2\2cd\7<\2\2d\16\3\2\2\2ef\7]\2\2f\20\3\2\2\2gh\7/\2\2hi\7Q"+
		"\2\2ij\7H\2\2jk\7<\2\2k\22\3\2\2\2lm\7_\2\2m\24\3\2\2\2no\7G\2\2op\7Z"+
		"\2\2pq\7C\2\2qr\7E\2\2rs\7V\2\2st\7N\2\2tu\7[\2\2uv\7/\2\2v\26\3\2\2\2"+
		"wx\7.\2\2x\30\3\2\2\2yz\7C\2\2z\32\3\2\2\2{|\7D\2\2|\34\3\2\2\2}~\7E\2"+
		"\2~\36\3\2\2\2\177\u0080\7F\2\2\u0080 \3\2\2\2\u0081\u0082\7G\2\2\u0082"+
		"\"\3\2\2\2\u0083\u0084\7H\2\2\u0084$\3\2\2\2\u0085\u0086\7I\2\2\u0086"+
		"&\3\2\2\2\u0087\u0088\7J\2\2\u0088(\3\2\2\2\u0089\u008a\7K\2\2\u008a*"+
		"\3\2\2\2\u008b\u008c\7L\2\2\u008c,\3\2\2\2\u008d\u008e\7M\2\2\u008e.\3"+
		"\2\2\2\u008f\u0090\7N\2\2\u0090\60\3\2\2\2\u0091\u0092\7O\2\2\u0092\62"+
		"\3\2\2\2\u0093\u0094\7P\2\2\u0094\64\3\2\2\2\u0095\u0096\7Q\2\2\u0096"+
		"\66\3\2\2\2\u0097\u0098\7R\2\2\u00988\3\2\2\2\u0099\u009a\7S\2\2\u009a"+
		":\3\2\2\2\u009b\u009c\7T\2\2\u009c<\3\2\2\2\u009d\u009e\7U\2\2\u009e>"+
		"\3\2\2\2\u009f\u00a0\7V\2\2\u00a0@\3\2\2\2\u00a1\u00a2\7W\2\2\u00a2B\3"+
		"\2\2\2\u00a3\u00a4\7X\2\2\u00a4D\3\2\2\2\u00a5\u00a6\7Y\2\2\u00a6F\3\2"+
		"\2\2\u00a7\u00a8\7Z\2\2\u00a8H\3\2\2\2\u00a9\u00aa\7[\2\2\u00aaJ\3\2\2"+
		"\2\u00ab\u00ac\7\\\2\2\u00acL\3\2\2\2\u00ad\u00ae\7/\2\2\u00aeN\3\2\2"+
		"\2\u00af\u00b0\7\60\2\2\u00b0P\3\2\2\2\u00b1\u00b2\7,\2\2\u00b2R\3\2\2"+
		"\2\u00b3\u00b4\7Q\2\2\u00b4\u00b5\7T\2\2\u00b5\u00b6\7H\2\2\u00b6\u00b7"+
		"\7\63\2\2\u00b7\u00da\7C\2\2\u00b8\u00b9\7Q\2\2\u00b9\u00ba\7T\2\2\u00ba"+
		"\u00bb\7H\2\2\u00bb\u00bc\7\63\2\2\u00bc\u00da\7D\2\2\u00bd\u00be\7Q\2"+
		"\2\u00be\u00bf\7T\2\2\u00bf\u00c0\7H\2\2\u00c0\u00c1\7\65\2\2\u00c1\u00da"+
		"\7C\2\2\u00c2\u00c3\7Q\2\2\u00c3\u00c4\7T\2\2\u00c4\u00c5\7H\2\2\u00c5"+
		"\u00da\78\2\2\u00c6\u00c7\7Q\2\2\u00c7\u00c8\7T\2\2\u00c8\u00c9\7H\2\2"+
		"\u00c9\u00ca\79\2\2\u00ca\u00da\7C\2\2\u00cb\u00cc\7Q\2\2\u00cc\u00cd"+
		"\7T\2\2\u00cd\u00ce\7H\2\2\u00ce\u00cf\79\2\2\u00cf\u00da\7D\2\2\u00d0"+
		"\u00d1\7Q\2\2\u00d1\u00d2\7T\2\2\u00d2\u00d3\7H\2\2\u00d3\u00da\7:\2\2"+
		"\u00d4\u00d5\7Q\2\2\u00d5\u00d6\7T\2\2\u00d6\u00d7\7H\2\2\u00d7\u00d8"+
		"\7;\2\2\u00d8\u00da\7D\2\2\u00d9\u00b3\3\2\2\2\u00d9\u00b8\3\2\2\2\u00d9"+
		"\u00bd\3\2\2\2\u00d9\u00c2\3\2\2\2\u00d9\u00c6\3\2\2\2\u00d9\u00cb\3\2"+
		"\2\2\u00d9\u00d0\3\2\2\2\u00d9\u00d4\3\2\2\2\u00daT\3\2\2\2\u00db\u00dc"+
		"\t\2\2\2\u00dcV\3\2\2\2\u00dd\u00de\t\3\2\2\u00de\u00df\3\2\2\2\u00df"+
		"\u00e0\b,\2\2\u00e0X\3\2\2\2\4\2\u00d9\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
