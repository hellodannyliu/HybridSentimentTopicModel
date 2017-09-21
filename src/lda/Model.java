package lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

public class Model {	

	public static String tassignSuffix;
	public static String thetaSuffix;
	public static String phiSuffix;
	public static String othersSuffix;
	public static String twordsSuffix;

	public String wordMapFile;
	public String trainlogFile;
	
	public String dir;
	public String dfile;
	public String modelName;
	public int modelStatus;
	public LDADataset data;
	
	public int M;
	public int V;
	public int K;
	public double alpha, beta;
	public int niters;
	public int liter;
	public int savestep;
	public int twords;
	public int withrawdata;

	public double [][] theta;
	public double [][] phi;

	public Vector<Integer> [] z;
	protected int [][] nw;
	protected int [][] nd;
	protected int [] nwsum;
	protected int [] ndsum;

	protected double [] p; 

	public Model(){
		setDefaultValues();	
	}

	public void setDefaultValues(){
		wordMapFile = "wordmap.txt";
		trainlogFile = "trainlog.txt";
		tassignSuffix = ".tassign";
		thetaSuffix = ".theta";
		phiSuffix = ".phi";
		othersSuffix = ".others";
		twordsSuffix = ".twords";
		
		dir = "./";
		dfile = "trndocs.dat";
		modelName = "model-final";
		modelStatus = Constants.MODEL_STATUS_UNKNOWN;		
		
		M = 0;
		V = 0;
		K = 100;
		alpha = 50.0 / K;
		beta = 0.1;
		niters = 2000;
		liter = 0;
		
		z = null;
		nw = null;
		nd = null;
		nwsum = null;
		ndsum = null;
		theta = null;
		phi = null;
	}

	protected boolean readOthersFile(String otherFile){
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(otherFile));
			String line;
			while((line = reader.readLine()) != null){
				StringTokenizer tknr = new StringTokenizer(line,"= \t\r\n");
				
				int count = tknr.countTokens();
				if (count != 2)
					continue;
				
				String optstr = tknr.nextToken();
				String optval = tknr.nextToken();
				
				if (optstr.equalsIgnoreCase("alpha")){
					alpha = Double.parseDouble(optval);					
				}
				else if (optstr.equalsIgnoreCase("beta")){
					beta = Double.parseDouble(optval);
				}
				else if (optstr.equalsIgnoreCase("ntopics")){
					K = Integer.parseInt(optval);
				}
				else if (optstr.equalsIgnoreCase("liter")){
					liter = Integer.parseInt(optval);
				}
				else if (optstr.equalsIgnoreCase("nwords")){
					V = Integer.parseInt(optval);
				}
				else if (optstr.equalsIgnoreCase("ndocs")){
					M = Integer.parseInt(optval);
				}
				else {

				}
			}
			
			reader.close();
		}
		catch (Exception e){
			System.out.println("Error while reading other file:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	protected boolean readTAssignFile(String tassignFile){
		try {
			int i,j;
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(tassignFile), "UTF-8"));
			
			String line;
			z = new Vector[M];			
			data = new LDADataset(M);
			data.V = V;			
			for (i = 0; i < M; i++){
				line = reader.readLine();
				StringTokenizer tknr = new StringTokenizer(line, " \t\r\n");
				
				int length = tknr.countTokens();
				
				Vector<Integer> words = new Vector<Integer>();
				Vector<Integer> topics = new Vector<Integer>();
				
				for (j = 0; j < length; j++){
					String token = tknr.nextToken();
					
					StringTokenizer tknr2 = new StringTokenizer(token, ":");
					if (tknr2.countTokens() != 2){
						System.out.println("Invalid word-topic assignment line\n");
						return false;
					}
					
					words.add(Integer.parseInt(tknr2.nextToken()));
					topics.add(Integer.parseInt(tknr2.nextToken()));
				}

				Document doc = new Document(words);
				data.setDoc(doc, i);

				z[i] = new Vector<Integer>();
				for (j = 0; j < topics.size(); j++){
					z[i].add(topics.get(j));
				}
				
			}
			
			reader.close();
		}
		catch (Exception e){
			System.out.println("Error while loading model: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean loadModel(){
		if (!readOthersFile(dir + File.separator + modelName + othersSuffix))
			return false;
		
		if (!readTAssignFile(dir + File.separator + modelName + tassignSuffix))
			return false;

		Dictionary dict = new Dictionary();
		if (!dict.readWordMap(dir + File.separator + wordMapFile))
			return false;
			
		data.localDict = dict;
		
		return true;
	}

	public boolean saveModelTAssign(String filename){
		int i, j;
		
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			for (i = 0; i < data.M; i++){
				for (j = 0; j < data.docs[i].length; ++j){
					writer.write(data.docs[i].words[j] + ":" + z[i].get(j) + " ");					
				}
				writer.write("\n");
			}
				
			writer.close();
		}
		catch (Exception e){
			System.out.println("Error while saving model tassign: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean saveModelTheta(String filename){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			for (int i = 0; i < M; i++){
				for (int j = 0; j < K; j++){
					writer.write(theta[i][j] + " ");
				}
				writer.write("\n");
			}
			writer.close();
		}
		catch (Exception e){
			System.out.println("Error while saving topic distribution file for this model: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean saveModelPhi(String filename){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			
			for (int i = 0; i < K; i++){
				for (int j = 0; j < V; j++){
					//writer.write(phi[i][j] + " "); //Change this to print E notation number.
					writer.write(new BigDecimal(phi[i][j]).toPlainString() + " ");
				}
				writer.write("\n");
			}
			writer.close();
		}
		catch (Exception e){
			System.out.println("Error while saving word-topic distribution:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean saveModelOthers(String filename){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			
			writer.write("alpha=" + alpha + "\n");
			writer.write("beta=" + beta + "\n");
			writer.write("ntopics=" + K + "\n");
			writer.write("ndocs=" + M + "\n");
			writer.write("nwords=" + V + "\n");
			writer.write("liters=" + liter + "\n");
			
			writer.close();
		}
		catch(Exception e){
			System.out.println("Error while saving model others:" + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean saveModelTwords(String filename){
		try{
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));
			
			if (twords > V){
				twords = V;
			}
			
			for (int k = 0; k < K; k++){
				List<Pair> wordsProbsList = new ArrayList<Pair>(); 
				for (int w = 0; w < V; w++){
					Pair p = new Pair(w, phi[k][w], false);
					
					wordsProbsList.add(p);
				}

				writer.write("Topic " + k + "th:\n");
				Collections.sort(wordsProbsList);
				
				for (int i = 0; i < twords; i++){
					if (data.localDict.contains((Integer)wordsProbsList.get(i).first)){
						String word = data.localDict.getWord((Integer)wordsProbsList.get(i).first);
						
						writer.write("\t" + word + " " + wordsProbsList.get(i).second + "\n");
					}
				}
			}
						
			writer.close();
		}
		catch(Exception e){
			System.out.println("Error while saving model twords: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean saveModel(String modelName){
		if (!saveModelTAssign(dir + File.separator + modelName + tassignSuffix)){
			return false;
		}
		
		if (!saveModelOthers(dir + File.separator + modelName + othersSuffix)){			
			return false;
		}
		
		if (!saveModelTheta(dir + File.separator + modelName + thetaSuffix)){
			return false;
		}
		
		if (!saveModelPhi(dir + File.separator + modelName + phiSuffix)){
			return false;
		}
		
		if (twords > 0){
			if (!saveModelTwords(dir + File.separator + modelName + twordsSuffix))
				return false;
		}
		return true;
	}

	protected boolean init(LDACmdOption option){		
		if (option == null)
			return false;
		
		modelName = option.modelName;
		K = option.K;
		
		alpha = option.alpha;
		if (alpha < 0.0)
			alpha = 50.0 / K;
		
		if (option.beta >= 0)
			beta = option.beta;
		
		niters = option.niters;
		
		dir = option.dir;
		if (dir.endsWith(File.separator))
			dir = dir.substring(0, dir.length() - 1);
		
		dfile = option.dfile;
		twords = option.twords;
		wordMapFile = option.wordMapFileName;
		
		return true;
	}

	public boolean initNewModel(LDACmdOption option){
		if (!init(option))
			return false;
		
		int m, n, w, k;		
		p = new double[K];		
		
		data = LDADataset.readDataSet(dir + File.separator + dfile);
		if (data == null){
			System.out.println("Fail to read training data!\n");
			return false;
		}

		M = data.M;
		V = data.V;
		dir = option.dir;
		savestep = option.savestep;

		nw = new int[V][K];
		for (w = 0; w < V; w++){
			for (k = 0; k < K; k++){
				nw[w][k] = 0;
			}
		}
		
		nd = new int[M][K];
		for (m = 0; m < M; m++){
			for (k = 0; k < K; k++){
				nd[m][k] = 0;
			}
		}
		
		nwsum = new int[K];
		for (k = 0; k < K; k++){
			nwsum[k] = 0;
		}
		
		ndsum = new int[M];
		for (m = 0; m < M; m++){
			ndsum[m] = 0;
		}
		
		z = new Vector[M];
		for (m = 0; m < data.M; m++){
			int N = data.docs[m].length;
			z[m] = new Vector<Integer>();

			for (n = 0; n < N; n++){
				int topic = (int)Math.floor(Math.random() * K);
				z[m].add(topic);
				nw[data.docs[m].words[n]][topic] += 1;
				nd[m][topic] += 1;
				nwsum[topic] += 1;
			}
			ndsum[m] = N;
		}
		
		theta = new double[M][K];		
		phi = new double[K][V];
		
		return true;
	}

	public boolean initNewModel(LDACmdOption option, LDADataset newData, Model trnModel){
		if (!init(option))
			return false;
		
		int m, n, w, k;
		
		K = trnModel.K;
		alpha = trnModel.alpha;
		beta = trnModel.beta;		
		
		p = new double[K];
		System.out.println("K:" + K);
		
		data = newData;

		M = data.M;
		V = data.V;
		dir = option.dir;
		savestep = option.savestep;
		System.out.println("M:" + M);
		System.out.println("V:" + V);

		nw = new int[V][K];
		for (w = 0; w < V; w++){
			for (k = 0; k < K; k++){
				nw[w][k] = 0;
			}
		}
		
		nd = new int[M][K];
		for (m = 0; m < M; m++){
			for (k = 0; k < K; k++){
				nd[m][k] = 0;
			}
		}
		
		nwsum = new int[K];
		for (k = 0; k < K; k++){
			nwsum[k] = 0;
		}
		
		ndsum = new int[M];
		for (m = 0; m < M; m++){
			ndsum[m] = 0;
		}
		
		z = new Vector[M];
		for (m = 0; m < data.M; m++){
			int N = data.docs[m].length;
			z[m] = new Vector<Integer>();

			for (n = 0; n < N; n++){
				int topic = (int)Math.floor(Math.random() * K);
				z[m].add(topic);
				nw[data.docs[m].words[n]][topic] += 1;
				nd[m][topic] += 1;
				nwsum[topic] += 1;
			}
			ndsum[m] = N;
		}
		
		theta = new double[M][K];		
		phi = new double[K][V];
		
		return true;
	}

	public boolean initNewModel(LDACmdOption option, Model trnModel){
		if (!init(option))
			return false;
		
		LDADataset dataset = LDADataset.readDataSet(dir + File.separator + dfile, trnModel.data.localDict);
		if (dataset == null){
			System.out.println("Fail to read dataset!\n");
			return false;
		}
		
		return initNewModel(option, dataset , trnModel);
	}

	public boolean initEstimatedModel(LDACmdOption option){
		if (!init(option))
			return false;
		
		int m, n, w, k;
		
		p = new double[K];

		if (!loadModel()){
			System.out.println("Fail to load word-topic assignment file of the model!\n");
			return false;
		}
		
		System.out.println("Model loaded:");
		System.out.println("\talpha:" + alpha);
		System.out.println("\tbeta:" + beta);
		System.out.println("\tM:" + M);
		System.out.println("\tV:" + V);		
		
		nw = new int[V][K];
		for (w = 0; w < V; w++){
			for (k = 0; k < K; k++){
				nw[w][k] = 0;
			}
		}
		
		nd = new int[M][K];
		for (m = 0; m < M; m++){
			for (k = 0; k < K; k++){
				nd[m][k] = 0;
			}
		}
		
		nwsum = new int[K];
	    for (k = 0; k < K; k++) {
		nwsum[k] = 0;
	    }
	    
	    ndsum = new int[M];
	    for (m = 0; m < M; m++) {
		ndsum[m] = 0;
	    }
	    
	    for (m = 0; m < data.M; m++){
	    	int N = data.docs[m].length;

	    	for (n = 0; n < N; n++){
	    		w = data.docs[m].words[n];
	    		int topic = (Integer)z[m].get(n);

	    		nw[w][topic] += 1;
	    		nd[m][topic] += 1;
	    		nwsum[topic] += 1;	    		
	    	}
	    	ndsum[m] = N;
	    }
	    
	    theta = new double[M][K];
	    phi = new double[K][V];
	    dir = option.dir;
		savestep = option.savestep;
	    
		return true;
	}
	
}
