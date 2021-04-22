package org.jamdev.jdl4pam.animalSpot;

import java.util.ArrayList;

import org.jamdev.jdl4pam.transforms.DLTransform;
import org.jamdev.jdl4pam.transforms.DLTransformsFactory;
import org.jamdev.jdl4pam.transforms.FreqTransform;
import org.jamdev.jdl4pam.transforms.WaveTransform;
import org.jamdev.jdl4pam.utils.DLUtils;
import org.jamdev.jpamutils.JamArr;
import org.jamdev.jpamutils.wavFiles.AudioData;

/**
 * 
 * Run a bat deep learning algorithm on a segment of bat data. 
 * 
 * <p> Notw that this example is based on the AWS djl library. 
 *
 */
public class AnimalSpotBatTest {

	public static void main( String[] args ) {


		//Path to the wav file 
		//		String wavFilePath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/BAT/example_wav/SKOVSOE_20200817_011402.wav"; 

		//High...ish SNR bat click
		//		String wavFilePath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/training_clips__Troldekær_deployment_3/DUB_20200623_000152_885.wav";
		//		String wavFilePath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/training_clips/noise/NOISE_20180816_205841_000.wav";
		//		String wavFilePath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/training_clips__Troldekær_deployment_3/DUB_20200623_003257_654.wav";
		//		int[] samplesChunk = new int[] {1536, 2810}; // the sample chunk to use. 
		//		int[] samplesChunk = new int[] {1024, 2298}; // the sample chunk to use. 
		String wavFilePath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/BAT/example_wav/call_393_2019_S4U05619MOL2-20180917-051012_2525_2534.wav";

		int[] samplesChunk = new int[] {0, 1274}; // the sample chunk to use. 

		//Path to the model
		//		String modelPath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/BAT/models/bats_dummy/BAT_4ms_256ft_8hop_128_NOISEAUG_40000_100000_-100_0_256000.pk";
		//		String modelPath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/BAT/models/bats_denmark/BAT_4ms_256ft_8hop_128_NOISEAUG_40000_100000_-100_0_256000_JAMIE.pk"; 
		//		String modelPath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/BAT/models/bats_denmark/BAT_4ms_256ft_8hop_128_NOISEAUG_40000_100000_-100_0_256000_JIT.pk";
		//String modelPath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/BAT/models/BAT_MODEL2/BAT_4ms_256ft_8hop_NOISEAUG_40000_100000_-100_0_256000.pk"; 
		//String modelPath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/BAT/models/BAT_MODEL2/BAT_4ms_256ft_8hop_128_NOISEAUG_40000_100000_-100_0_256000.pk.pk"; 
		String modelPath = "/Users/au671271/Google Drive/Aarhus_research/PAMGuard_bats_2020/deep_learning/BAT/models/BAT_MODEL_3/BAT_JAMIE_4ms_256fft_8hop_-100_20_15_60_128_256_NOJIT_BAT_DATA_NAUG_V1_JIT.pk"; 

		//wav file 
		try {			
			//first open the model and get the correct parameters. 
			AnimalSpotModel soundSpotModel = new AnimalSpotModel(modelPath); 

			//System.out.println(soundSpotModel.getRawParamsString());
			//create the DL params. 
			AnimalSpotParams dlParams = new AnimalSpotParams(soundSpotModel.getTransformsString());

			//Open wav files. 
			AudioData soundData = DLUtils.loadWavFile(wavFilePath);
			soundData = soundData.trim(samplesChunk[0], samplesChunk[1]); 

			//generate the transforms. 
			ArrayList<DLTransform> transforms =	DLTransformsFactory.makeDLTransforms(dlParams.dlTransforms); 

			System.out.println(dlParams.toString());

			((WaveTransform) transforms.get(0)).setWaveData(soundData); 

			DLTransform transform = transforms.get(0); 
			for (int i=0; i<transforms.size(); i++) {
//				System.out.println(transforms); 
				transform = transforms.get(i).transformData(transform); 
			}
			
			double[][] dataTest = ((FreqTransform) transform).getSpecTransfrom().getTransformedData(); 
			System.out.println("Data min:  " + JamArr.min(dataTest) +  " max:  " +  JamArr.max(dataTest));

			float[] output = null; 
			float[][][] data;
			int nStack = 1; //number of specs to give to the classifier. 
			for (int i=0; i<10; i++) {
				long time1 = System.currentTimeMillis();
				data = new float[nStack][][]; 
				for (int j=0; j<nStack; j++) {
					data[j] = DLUtils.toFloatArray(((FreqTransform) transform).getSpecTransfrom().getTransformedData()); 
				}
				output = soundSpotModel.runModel(data); 
				long time2 = System.currentTimeMillis();
				System.out.println("Time to run model: " + (time2-time1) + " ms"); 
			}

			double[] prob = new double[output.length]; 
			for (int j=0; j<output.length; j++) {
				//python code for this. 
				//		    	prob = torch.nn.functional.softmax(out).numpy()[n, 1]
				//	                    pred = int(prob >= ARGS.threshold)		    	
				//softmax function
				prob[j] = DLUtils.softmax(output[j], output); 
				System.out.println("The probability is: " + prob[j]); 
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

	}



}
