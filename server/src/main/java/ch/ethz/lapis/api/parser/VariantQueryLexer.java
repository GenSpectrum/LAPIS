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
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, A=15, B=16, C=17, D=18,
		E=19, F=20, G=21, H=22, I=23, J=24, K=25, L=26, M=27, N=28, O=29, P=30,
		Q=31, R=32, S=33, T=34, U=35, V=36, W=37, X=38, Y=39, Z=40, MINUS=41,
		DOT=42, ASTERISK=43, ORF=44, NUMBER=45, WHITESPACE=46;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8",
			"T__9", "T__10", "T__11", "T__12", "T__13", "A", "B", "C", "D", "E",
			"F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
			"T", "U", "V", "W", "X", "Y", "Z", "MINUS", "DOT", "ASTERISK", "ORF",
			"NUMBER", "WHITESPACE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'!'", "'&'", "'|'", "'('", "')'", "':'", "'INS_'", "'?'", "'TEMPORARY_NODE_'",
			"'['", "'-OF:'", "']'", "'EXACTLY-'", "','", "'A'", "'B'", "'C'", "'D'",
			"'E'", "'F'", "'G'", "'H'", "'I'", "'J'", "'K'", "'L'", "'M'", "'N'",
			"'O'", "'P'", "'Q'", "'R'", "'S'", "'T'", "'U'", "'V'", "'W'", "'X'",
			"'Y'", "'Z'", "'-'", "'.'", "'*'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null,
			null, null, null, "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
			"L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y",
			"Z", "MINUS", "DOT", "ASTERISK", "ORF", "NUMBER", "WHITESPACE"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\60\u00fe\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7"+
		"\3\7\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3"+
		"\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21"+
		"\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30"+
		"\3\31\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37"+
		"\3 \3 \3!\3!\3\"\3\"\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'\3(\3(\3)\3)\3*\3"+
		"*\3+\3+\3,\3,\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3"+
		"-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\5-\u00f7\n"+
		"-\3.\3.\3/\3/\3/\3/\2\2\60\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25"+
		"\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32"+
		"\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60\3\2\4"+
		"\3\2\62;\5\2\13\f\17\17\"\"\2\u0104\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2"+
		"\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23"+
		"\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2"+
		"\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2"+
		"\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3"+
		"\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2"+
		"\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2"+
		"\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2["+
		"\3\2\2\2\2]\3\2\2\2\3_\3\2\2\2\5a\3\2\2\2\7c\3\2\2\2\te\3\2\2\2\13g\3"+
		"\2\2\2\ri\3\2\2\2\17k\3\2\2\2\21p\3\2\2\2\23r\3\2\2\2\25\u0082\3\2\2\2"+
		"\27\u0084\3\2\2\2\31\u0089\3\2\2\2\33\u008b\3\2\2\2\35\u0094\3\2\2\2\37"+
		"\u0096\3\2\2\2!\u0098\3\2\2\2#\u009a\3\2\2\2%\u009c\3\2\2\2\'\u009e\3"+
		"\2\2\2)\u00a0\3\2\2\2+\u00a2\3\2\2\2-\u00a4\3\2\2\2/\u00a6\3\2\2\2\61"+
		"\u00a8\3\2\2\2\63\u00aa\3\2\2\2\65\u00ac\3\2\2\2\67\u00ae\3\2\2\29\u00b0"+
		"\3\2\2\2;\u00b2\3\2\2\2=\u00b4\3\2\2\2?\u00b6\3\2\2\2A\u00b8\3\2\2\2C"+
		"\u00ba\3\2\2\2E\u00bc\3\2\2\2G\u00be\3\2\2\2I\u00c0\3\2\2\2K\u00c2\3\2"+
		"\2\2M\u00c4\3\2\2\2O\u00c6\3\2\2\2Q\u00c8\3\2\2\2S\u00ca\3\2\2\2U\u00cc"+
		"\3\2\2\2W\u00ce\3\2\2\2Y\u00f6\3\2\2\2[\u00f8\3\2\2\2]\u00fa\3\2\2\2_"+
		"`\7#\2\2`\4\3\2\2\2ab\7(\2\2b\6\3\2\2\2cd\7~\2\2d\b\3\2\2\2ef\7*\2\2f"+
		"\n\3\2\2\2gh\7+\2\2h\f\3\2\2\2ij\7<\2\2j\16\3\2\2\2kl\7K\2\2lm\7P\2\2"+
		"mn\7U\2\2no\7a\2\2o\20\3\2\2\2pq\7A\2\2q\22\3\2\2\2rs\7V\2\2st\7G\2\2"+
		"tu\7O\2\2uv\7R\2\2vw\7Q\2\2wx\7T\2\2xy\7C\2\2yz\7T\2\2z{\7[\2\2{|\7a\2"+
		"\2|}\7P\2\2}~\7Q\2\2~\177\7F\2\2\177\u0080\7G\2\2\u0080\u0081\7a\2\2\u0081"+
		"\24\3\2\2\2\u0082\u0083\7]\2\2\u0083\26\3\2\2\2\u0084\u0085\7/\2\2\u0085"+
		"\u0086\7Q\2\2\u0086\u0087\7H\2\2\u0087\u0088\7<\2\2\u0088\30\3\2\2\2\u0089"+
		"\u008a\7_\2\2\u008a\32\3\2\2\2\u008b\u008c\7G\2\2\u008c\u008d\7Z\2\2\u008d"+
		"\u008e\7C\2\2\u008e\u008f\7E\2\2\u008f\u0090\7V\2\2\u0090\u0091\7N\2\2"+
		"\u0091\u0092\7[\2\2\u0092\u0093\7/\2\2\u0093\34\3\2\2\2\u0094\u0095\7"+
		".\2\2\u0095\36\3\2\2\2\u0096\u0097\7C\2\2\u0097 \3\2\2\2\u0098\u0099\7"+
		"D\2\2\u0099\"\3\2\2\2\u009a\u009b\7E\2\2\u009b$\3\2\2\2\u009c\u009d\7"+
		"F\2\2\u009d&\3\2\2\2\u009e\u009f\7G\2\2\u009f(\3\2\2\2\u00a0\u00a1\7H"+
		"\2\2\u00a1*\3\2\2\2\u00a2\u00a3\7I\2\2\u00a3,\3\2\2\2\u00a4\u00a5\7J\2"+
		"\2\u00a5.\3\2\2\2\u00a6\u00a7\7K\2\2\u00a7\60\3\2\2\2\u00a8\u00a9\7L\2"+
		"\2\u00a9\62\3\2\2\2\u00aa\u00ab\7M\2\2\u00ab\64\3\2\2\2\u00ac\u00ad\7"+
		"N\2\2\u00ad\66\3\2\2\2\u00ae\u00af\7O\2\2\u00af8\3\2\2\2\u00b0\u00b1\7"+
		"P\2\2\u00b1:\3\2\2\2\u00b2\u00b3\7Q\2\2\u00b3<\3\2\2\2\u00b4\u00b5\7R"+
		"\2\2\u00b5>\3\2\2\2\u00b6\u00b7\7S\2\2\u00b7@\3\2\2\2\u00b8\u00b9\7T\2"+
		"\2\u00b9B\3\2\2\2\u00ba\u00bb\7U\2\2\u00bbD\3\2\2\2\u00bc\u00bd\7V\2\2"+
		"\u00bdF\3\2\2\2\u00be\u00bf\7W\2\2\u00bfH\3\2\2\2\u00c0\u00c1\7X\2\2\u00c1"+
		"J\3\2\2\2\u00c2\u00c3\7Y\2\2\u00c3L\3\2\2\2\u00c4\u00c5\7Z\2\2\u00c5N"+
		"\3\2\2\2\u00c6\u00c7\7[\2\2\u00c7P\3\2\2\2\u00c8\u00c9\7\\\2\2\u00c9R"+
		"\3\2\2\2\u00ca\u00cb\7/\2\2\u00cbT\3\2\2\2\u00cc\u00cd\7\60\2\2\u00cd"+
		"V\3\2\2\2\u00ce\u00cf\7,\2\2\u00cfX\3\2\2\2\u00d0\u00d1\7Q\2\2\u00d1\u00d2"+
		"\7T\2\2\u00d2\u00d3\7H\2\2\u00d3\u00d4\7\63\2\2\u00d4\u00f7\7C\2\2\u00d5"+
		"\u00d6\7Q\2\2\u00d6\u00d7\7T\2\2\u00d7\u00d8\7H\2\2\u00d8\u00d9\7\63\2"+
		"\2\u00d9\u00f7\7D\2\2\u00da\u00db\7Q\2\2\u00db\u00dc\7T\2\2\u00dc\u00dd"+
		"\7H\2\2\u00dd\u00de\7\65\2\2\u00de\u00f7\7C\2\2\u00df\u00e0\7Q\2\2\u00e0"+
		"\u00e1\7T\2\2\u00e1\u00e2\7H\2\2\u00e2\u00f7\78\2\2\u00e3\u00e4\7Q\2\2"+
		"\u00e4\u00e5\7T\2\2\u00e5\u00e6\7H\2\2\u00e6\u00e7\79\2\2\u00e7\u00f7"+
		"\7C\2\2\u00e8\u00e9\7Q\2\2\u00e9\u00ea\7T\2\2\u00ea\u00eb\7H\2\2\u00eb"+
		"\u00ec\79\2\2\u00ec\u00f7\7D\2\2\u00ed\u00ee\7Q\2\2\u00ee\u00ef\7T\2\2"+
		"\u00ef\u00f0\7H\2\2\u00f0\u00f7\7:\2\2\u00f1\u00f2\7Q\2\2\u00f2\u00f3"+
		"\7T\2\2\u00f3\u00f4\7H\2\2\u00f4\u00f5\7;\2\2\u00f5\u00f7\7D\2\2\u00f6"+
		"\u00d0\3\2\2\2\u00f6\u00d5\3\2\2\2\u00f6\u00da\3\2\2\2\u00f6\u00df\3\2"+
		"\2\2\u00f6\u00e3\3\2\2\2\u00f6\u00e8\3\2\2\2\u00f6\u00ed\3\2\2\2\u00f6"+
		"\u00f1\3\2\2\2\u00f7Z\3\2\2\2\u00f8\u00f9\t\2\2\2\u00f9\\\3\2\2\2\u00fa"+
		"\u00fb\t\3\2\2\u00fb\u00fc\3\2\2\2\u00fc\u00fd\b/\2\2\u00fd^\3\2\2\2\4"+
		"\2\u00f6\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
