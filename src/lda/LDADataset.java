package lda;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class LDADataset {

	public Dictionary localDict;
	public Document [] docs;
	public int M;
	public int V;

	public Map<Integer, Integer> lid2gid; 

	public Dictionary globalDict;	 		

	public LDADataset(){
		localDict = new Dictionary();
		M = 0;
		V = 0;
		docs = null;
	
		globalDict = null;
		lid2gid = null;
	}
	
	public LDADataset(int M){
		localDict = new Dictionary();
		this.M = M;
		this.V = 0;
		docs = new Document[M];	
		
		globalDict = null;
		lid2gid = null;
	}
	
	public LDADataset(int M, Dictionary globalDict){
		localDict = new Dictionary();	
		this.M = M;
		this.V = 0;
		docs = new Document[M];	
		
		this.globalDict = globalDict;
		lid2gid = new HashMap<Integer, Integer>();
	}

	public void setDoc(Document doc, int idx){
		if (0 <= idx && idx < M){
			docs[idx] = doc;
		}
	}

	public void setDoc(String str, int idx){
		if (0 <= idx && idx < M){
			String [] words = str.split("[ \\t\\n]");
			
			Vector<Integer> ids = new Vector<Integer>();
			
			for (String word : words){
				int _id = localDict.word2id.size();
				
				if (localDict.contains(word))		
					_id = localDict.getID(word);
								
				if (globalDict != null){
					Integer id = globalDict.getID(word);
					
					if (id != null){
						localDict.addWord(word);
						
						lid2gid.put(_id, id);
						ids.add(_id);
					}
					else {
					}
				}
				else {
					localDict.addWord(word);
					ids.add(_id);
				}
			}
			
			Document doc = new Document(ids, str);
			docs[idx] = doc;
			V = localDict.word2id.size();			
		}
	}

	public static LDADataset readDataSet(String filename){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));
			
			LDADataset data = readDataSet(reader);
			
			reader.close();
			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static LDADataset readDataSet(String filename, Dictionary dict){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));
			LDADataset data = readDataSet(reader, dict);
			
			reader.close();
			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static LDADataset readDataSet(BufferedReader reader){
		try {
			String line;
			line = reader.readLine();
			int M = Integer.parseInt(line);
			
			LDADataset data = new LDADataset(M);
			for (int i = 0; i < M; ++i){
				line = reader.readLine();
				
				data.setDoc(line, i);
			}
			
			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static LDADataset readDataSet(BufferedReader reader, Dictionary dict){
		try {
			String line;
			line = reader.readLine();
			int M = Integer.parseInt(line);
			System.out.println("NewM:" + M);
			
			LDADataset data = new LDADataset(M, dict);
			for (int i = 0; i < M; ++i){
				line = reader.readLine();
				
				data.setDoc(line, i);
			}
			
			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static LDADataset readDataSet(String [] strs){
		LDADataset data = new LDADataset(strs.length);
		
		for (int i = 0 ; i < strs.length; ++i){
			data.setDoc(strs[i], i);
		}
		return data;
	}

	public static LDADataset readDataSet(String [] strs, Dictionary dict){ ;
		LDADataset data = new LDADataset(strs.length, dict);
		
		for (int i = 0 ; i < strs.length; ++i){
			data.setDoc(strs[i], i);
		}
		return data;
	}
}
