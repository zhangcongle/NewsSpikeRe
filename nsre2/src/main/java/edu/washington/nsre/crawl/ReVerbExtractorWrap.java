package edu.washington.nsre.crawl;

/* For representing a sentence that is annotated with pos tags and np chunks.*/
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import edu.washington.nsre.util.*;

import edu.washington.cs.knowitall.nlp.ChunkedSentence;

/* String -> ChunkedSentence */
import edu.washington.cs.knowitall.nlp.OpenNlpSentenceChunker;

/* The class that is responsible for extraction. */
import edu.washington.cs.knowitall.commonlib.Range;
import edu.washington.cs.knowitall.extractor.ReVerbExtractor;

/* The class that is responsible for assigning a confidence score to an 
 * extraction.
 */
import edu.washington.cs.knowitall.extractor.conf.ReVerbConfFunction;

/* A class for holding a (arg1, rel, arg2) triple. */
import edu.washington.cs.knowitall.nlp.extraction.ChunkedArgumentExtraction;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;
import edu.washington.cs.knowitall.normalization.BinaryExtractionNormalizer;
import edu.washington.cs.knowitall.normalization.HeadNounExtractor;
import edu.washington.cs.knowitall.normalization.NormalizedBinaryExtraction;
import edu.washington.cs.knowitall.util.Morpha;

public class ReVerbExtractorWrap {

	OpenNlpSentenceChunker chunker;
	ReVerbExtractor reverb;
	ReVerbConfFunction confFunc;

	public ReVerbExtractorWrap() {
		try {
			chunker = new OpenNlpSentenceChunker();
			reverb = new ReVerbExtractor();
			// reverb.setAllowUnary(true);
			confFunc = new ReVerbConfFunction();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ReVerbResult parse(String raw_sentence) {
		ReVerbResult rvr = new ReVerbResult();
		rvr.raw_sentence = raw_sentence;
		rvr.chunk_sent = chunker.chunkSentence(raw_sentence);
		rvr.reverb_extract = new ArrayList<ChunkedBinaryExtraction>();
		for (ChunkedBinaryExtraction extr : reverb.extract(rvr.chunk_sent)) {
			rvr.reverb_extract.add(extr);
		}
		return rvr;
	}

	public ReVerbResult parse(String tokens_str, String posTags_str) {
		ReVerbResult rvr = new ReVerbResult();
		rvr.raw_sentence = tokens_str;
		String[] tokens = tokens_str.split(" ");
		String[] posTags = posTags_str.split(" ");
		rvr.chunk_sent = chunker.chunkSentence(tokens, posTags);
		rvr.reverb_extract = new ArrayList<ChunkedBinaryExtraction>();
		for (ChunkedBinaryExtraction extr : reverb.extract(rvr.chunk_sent)) {
			rvr.reverb_extract.add(extr);
		}
		return rvr;
	}

	public ReVerbResult parse(String[] tokens, String[] posTags) {
		ReVerbResult rvr = new ReVerbResult();
		rvr.raw_sentence = StringUtil.join(tokens, " ");
		
		rvr.chunk_sent = chunker.chunkSentence(tokens, posTags);
		rvr.reverb_extract = new ArrayList<ChunkedBinaryExtraction>();
		for (ChunkedBinaryExtraction extr : reverb.extract(rvr.chunk_sent)) {
			rvr.reverb_extract.add(extr);
		}
		return rvr;
	}

	private static void writeMyChunk(ChunkedSentence sent, int start, int end,
			String sentenceid, String chunktype, DW dw) {
		if (start == 16) {
			// D.p(start);
		}
		if (start != -1) {
			Range r = new Range(start, end - start);

			ChunkedSentence sub = sent.getSubSequence(r);
			dw.write(sentenceid, r.getStart(), r.getEnd(), chunktype,
					sub.getTokensAsString());
		}
	}

	/**
	 * .rvtkn: sentenceid \t token_pos_chunk_morpha .rvphrase: sentenceid \t
	 * phraseid \t start \t end \t VP/NP \t normalized text .rvrel: sentenceid
	 * \t {rel_start_end} {arg1_start_end} {arg2_start_end} rel arg1 arg2
	 * */
	public static void parseText(String input, String output_prefix) {
		ReVerbExtractorWrap rvew = new ReVerbExtractorWrap();
		DR dr = new DR(input);
		String[] l;
		DW dw_rvtkn = new DW(output_prefix + ".rvtkn");
		DW dw_rvphrase = new DW(output_prefix + ".rvphrase");
		DW dw_rvrel = new DW(output_prefix + ".rvrel");
		int count = 0;
		D.p("Time", new java.util.Date());
		Morpha lexer = new Morpha(new ByteArrayInputStream("".getBytes()));
		HeadNounExtractor headnoun_extractor = new HeadNounExtractor();
		BinaryExtractionNormalizer normalizer = new BinaryExtractionNormalizer();
		int phraseId = 0;
		while ((l = dr.read()) != null) {
			// if (count++ > 1000)
			// break;
			String sentenceid = l[0];
			String text = l[1];
			ReVerbResult rvr = rvew.parse(text);
			ChunkedSentence sent = rvr.chunk_sent;
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < rvr.chunk_sent.getLength(); i++) {
					String token = sent.getToken(i);
					String posTag = sent.getPosTag(i);
					String chunkTag = sent.getChunkTag(i);
					String wordTag = token.toLowerCase() + "_" + posTag;
					String tokenNorm = token.toLowerCase();
					try {
						lexer.yyreset(new StringReader(wordTag));
						lexer.yybegin(Morpha.scan);
						tokenNorm = lexer.next();
					} catch (Throwable e) {
						e.printStackTrace();
					}
					sb.append(token + "|" + posTag + "|" + chunkTag + "|"
							+ tokenNorm + " ");
				}
				dw_rvtkn.write(sentenceid, sb.toString());
			}
			{
				int last = -1;
				String chunktype = "";
				for (int i = 0; i < rvr.chunk_sent.getLength(); i++) {
					String token = sent.getToken(i);
					String posTag = sent.getPosTag(i);
					String chunkTag = sent.getChunkTag(i);
					if (chunkTag.startsWith("B-")) {
						writeMyChunk(sent, last, i, sentenceid, chunktype,
								dw_rvphrase);
						last = i;
						chunktype = chunkTag.substring(2);
					} else if (chunkTag.equals("O")) {
						writeMyChunk(sent, last, i, sentenceid, chunktype,
								dw_rvphrase);
						// writeMyChunk(sent, i, i + 1, sentenceid, "O",
						// dw_rvphrase);
						last = i;
						chunktype = "O";
					}
				}
				if (last > 0) {
					writeMyChunk(sent, last, rvr.chunk_sent.getLength(),
							sentenceid, chunktype, dw_rvphrase);
				}
			}
			// {
			// // #####NP chunks
			// for (Range r : sent.getNpChunkRanges()) {
			// ChunkedSentence sub = sent.getSubSequence(r);
			// dw_rvphrase.write(sentenceid, phraseId++, r.getStart(),
			// r.getEnd(), "NP", sub.getTokensAsString(),
			// sub.getTokenNormAsString());
			// // D.p(r, sub.toString(), sub.getTokenNormAsString(),
			// // sub.getPosTagsAsString());
			// }
			// // #####VP chunks
			// for (Range r : sent.getVpChunkRanges()) {
			// ChunkedSentence sub = sent.getSubSequence(r);
			// dw_rvphrase.write(sentenceid, phraseId++, r.getStart(),
			// r.getEnd(), "VP", sub.getTokensAsString(),
			// sub.getTokenNormAsString());
			// // D.p(r, sub.toString(), sub.getTokenNormAsString(),
			// // sub.getPosTagsAsString());
			// }
			// // D.p(sent.getNpChunkRanges());
			// }
			{

				for (ChunkedBinaryExtraction extr : rvr.reverb_extract) {
					NormalizedBinaryExtraction ne = normalizer.normalize(extr);
					dw_rvrel.write(sentenceid, extr.getArgument1().getRange()
							.getStart(), extr.getArgument1().getRange()
							.getEnd(),
							extr.getRelation().getRange().getStart(), extr
									.getRelation().getRange().getEnd(), extr
									.getArgument2().getRange().getStart(), extr
									.getArgument2().getRange().getEnd(), extr
									.getArgument1(), extr.getRelation(), extr
									.getArgument2(), headnoun_extractor
									.normalizeField(ne.getArgument1()), ne
									.getRelationNorm(), headnoun_extractor
									.normalizeField(ne.getArgument2()));
					// sb.append(extr.getRelation().getRange().getStart() + " "
					// +
					// extr.getRelation().getRange().getEnd() + " " +
					// extr.getArgument1().getRange().getStart() + " " +
					// extr.getArgument1().getRange().getEnd() + " " +
					// extr.getArgument2().getRange().getStart() + " " +
					// extr.getArgument2().getRange().getEnd());
					// dw_rvrel.write(id, extr.getRelation().getText(),
					// extr.getArgument1().getText(),
					// extr.getArgument2().getText(), sb.toString());
				}
			}
		}
		D.p("Time", new java.util.Date());
		dr.close();
		dw_rvtkn.close();
		dw_rvrel.close();
		dw_rvphrase.close();
	}

	public static void parseText(String input_tokens, String input_pos,
			String output_prefix) {
		ReVerbExtractorWrap rvew = new ReVerbExtractorWrap();

		// DW dw_rvtkn = new DW(output_prefix + ".rvtkn");
		DW dw_rvphrase = new DW(output_prefix + ".rvphrase");
		DW dw_rvrel = new DW(output_prefix + ".rvrel");
		int count = 0;
		D.p("Time", new java.util.Date());
		Morpha lexer = new Morpha(new ByteArrayInputStream("".getBytes()));
		HeadNounExtractor headnoun_extractor = new HeadNounExtractor();
		BinaryExtractionNormalizer normalizer = new BinaryExtractionNormalizer();
		int phraseId = 0;
		DR dr = new DR(input_tokens);
		DR drpos = new DR(input_pos);
		String[] l;
		String[] lpos = drpos.read();
		while ((l = dr.read()) != null) {
			// if (count++ > 1000)
			// break;
			int sentenceId = Integer.parseInt(l[0]);

			while (lpos != null && lpos.length > 1
					&& Integer.parseInt(lpos[0]) < sentenceId) {
				lpos = drpos.read();
			}
			if (lpos != null && lpos.length > 1
					&& Integer.parseInt(lpos[0]) == sentenceId) {
				String tokens_str = l[1];
				String posTags_str = lpos[1];
				ReVerbResult rvr = rvew.parse(tokens_str, posTags_str);
				ChunkedSentence sent = rvr.chunk_sent;
				// {
				// StringBuilder sb = new StringBuilder();
				// for (int i = 0; i < rvr.chunk_sent.getLength(); i++) {
				// String token = sent.getToken(i);
				// String posTag = sent.getPosTag(i);
				// String chunkTag = sent.getChunkTag(i);
				// String wordTag = token.toLowerCase() + "_" + posTag;
				// String tokenNorm = token.toLowerCase();
				// try {
				// lexer.yyreset(new StringReader(wordTag));
				// lexer.yybegin(Morpha.scan);
				// tokenNorm = lexer.next();
				// } catch (Throwable e) {
				// e.printStackTrace();
				// }
				// sb.append(token + "_" + posTag + "_" + chunkTag + "_" +
				// tokenNorm + " ");
				// }
				// dw_rvtkn.write(sentenceId, sb.toString());
				// }
				{
					// #####NP chunks
					for (Range r : sent.getNpChunkRanges()) {
						ChunkedSentence sub = sent.getSubSequence(r);
						dw_rvphrase.write(sentenceId, phraseId++, r.getStart(),
								r.getEnd(), "NP", sub.getTokensAsString(),
								sub.getTokenNormAsString());
						// D.p(r, sub.toString(), sub.getTokenNormAsString(),
						// sub.getPosTagsAsString());
					}
					// #####VP chunks
					for (Range r : sent.getVpChunkRanges()) {
						ChunkedSentence sub = sent.getSubSequence(r);
						dw_rvphrase.write(sentenceId, phraseId++, r.getStart(),
								r.getEnd(), "VP", sub.getTokensAsString(),
								sub.getTokenNormAsString());
						// D.p(r, sub.toString(), sub.getTokenNormAsString(),
						// sub.getPosTagsAsString());
					}
					// D.p(sent.getNpChunkRanges());
				}
				{
					for (ChunkedBinaryExtraction extr : rvr.reverb_extract) {
						NormalizedBinaryExtraction ne = normalizer
								.normalize(extr);
						dw_rvrel.write(sentenceId, extr.getArgument1()
								.getRange().getStart(), extr.getArgument1()
								.getRange().getEnd(), extr.getRelation()
								.getRange().getStart(), extr.getRelation()
								.getRange().getEnd(), extr.getArgument2()
								.getRange().getStart(), extr.getArgument2()
								.getRange().getEnd(), extr.getArgument1(), extr
								.getRelation(), extr.getArgument2(),
								headnoun_extractor.normalizeField(ne
										.getArgument1()), ne.getRelationNorm(),
								headnoun_extractor.normalizeField(ne
										.getArgument2()));
					}
				}
			}

		}
		D.p("Time", new java.util.Date());
		dr.close();
		// dw_rvtkn.close();
		dw_rvrel.close();
		dw_rvphrase.close();
	}

	public static void main_(String[] args) throws Exception {
		if (args.length == 2) {
			parseText(args[0], args[1]);
		} else if (args.length == 3) {
			parseText(args[0], args[1], args[2]);
		}
	}

	public static void main2(String[] args) throws Exception {

	}

	public static void main(String[] args) throws Exception {

		String sentStr = "Michael McGinn is the mayor of Seattle.";
		// String sentStr =
		// "Dan was born in USA.";
		// String sentStr =
		// "Bruising Rams give preview of coming NFC West battles";
		// Looks on the classpath for the default model files.
		Morpha lexer = new Morpha(new ByteArrayInputStream("".getBytes()));
		HeadNounExtractor he = new HeadNounExtractor();

		OpenNlpSentenceChunker chunker = new OpenNlpSentenceChunker();
		ChunkedSentence sent = chunker.chunkSentence(sentStr);
		{
			// what is the chunks of the sentence?
			sent.getNpChunkRanges();
			D.p("#####NP chunks");
			for (Range r : sent.getNpChunkRanges()) {
				ChunkedSentence sub = sent.getSubSequence(r);
				D.p(r, sub.toString(), sub.getTokenNormAsString(),
						sub.getPosTagsAsString());
			}
			D.p("#####VP chunks");
			for (Range r : sent.getVpChunkRanges()) {
				ChunkedSentence sub = sent.getSubSequence(r);
				D.p(r, sub.toString(), sub.getTokenNormAsString(),
						sub.getPosTagsAsString());
			}
			// D.p(sent.getNpChunkRanges());
		}
		BinaryExtractionNormalizer normalizer = new BinaryExtractionNormalizer();
		//
		// // Prints out the (token, tag, chunk-tag) for the sentence
		System.out.println(sentStr);

		for (int i = 0; i < sent.getLength(); i++) {
			String token = sent.getToken(i);
			String posTag = sent.getPosTag(i);
			String chunkTag = sent.getChunkTag(i);
			String wordTag = token.toLowerCase() + "_" + posTag;
			String tokenNorm = token.toLowerCase();
			try {
				lexer.yyreset(new StringReader(wordTag));
				lexer.yybegin(Morpha.scan);
				tokenNorm = lexer.next();

			} catch (Throwable e) {
				e.printStackTrace();
			}
			System.out.println(token + " " + posTag + " " + chunkTag + " "
					+ tokenNorm);
		}

		// Prints out extractions from the sentence.
		ReVerbExtractor reverb = new ReVerbExtractor();
		reverb.setAllowUnary(true);
		ReVerbConfFunction confFunc = new ReVerbConfFunction();
		for (ChunkedBinaryExtraction extr : reverb.extract(sent)) {
			double conf = confFunc.getConf(extr);
			NormalizedBinaryExtraction ne = normalizer.normalize(extr);

			System.out.println("Arg1=" + ne.getArgument1Norm() + "\t"
					+ he.normalizeField(ne.getArgument1()));
			System.out.println("Rel=" + ne.getRelationNorm());
			System.out.println("Arg2=" + ne.getArgument2Norm() + "\t"
					+ he.normalizeField(ne.getArgument2()));
			System.out.println("Conf=" + conf);
			// what is the head of argument?

			// System.out.println("Arg1=" + extr.getArgument1());
			// System.out.println("Rel=" + extr.getRelation());
			// System.out.println("Arg2=" + extr.getArgument2());
			// System.out.println("Conf=" + conf);
		}
	}
}
