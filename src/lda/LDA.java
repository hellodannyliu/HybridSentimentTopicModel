package lda;

import org.kohsuke.args4j.*;

public class LDA {
	
	public static void main(String args[]){
		String[] inlineArgs = {"-est", "-alpha", "0.5", "-beta", "0.01", "-ntopics", "50", "-niters", "2000", "-savestep", "50", "-twords", "20", "-dir", "C:\\data\\movies", "-dfile", "moviesFormatted.txt"};

		LDACmdOption option = new LDACmdOption();
		CmdLineParser parser = new CmdLineParser(option);
		
		try {
			//if (args.length == 0){ //Change from args.length.
			if (inlineArgs.length == 0){ //Change from args.length.
				showHelp(parser);
				return;
			}
			
			//parser.parseArgument(args); //Change from (args).
			parser.parseArgument(inlineArgs); //Change from (args).
			
			if (option.est || option.estc){
				Estimator estimator = new Estimator();
				estimator.init(option);
				estimator.estimate();
			}
			else if (option.inf){
				Inferencer inferencer = new Inferencer();
				inferencer.init(option);
				
				Model newModel = inferencer.inference();
			
				for (int i = 0; i < newModel.phi.length; ++i){
					//phi: K * V
					System.out.println("-----------------------\ntopic" + i  + " : ");
					for (int j = 0; j < 10; ++j){
						System.out.println(inferencer.globalDict.id2word.get(j) + "\t" + newModel.phi[i][j]);
					}
				}
			}
		}
		catch (CmdLineException cle){
			System.out.println("Command line error: " + cle.getMessage());
			showHelp(parser);
			return;
		}
		catch (Exception e){
			System.out.println("Error in main: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}
	
	public static void showHelp(CmdLineParser parser){
		System.out.println("LDA [options ...] [arguments...]");
		parser.printUsage(System.out);
	}
	
}
