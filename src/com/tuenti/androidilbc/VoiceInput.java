package com.tuenti.androidilbc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import com.tuenti.androidilbc.Codec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Records and stores audio input.
 * Thanks to a lil' help from:
 * http://stackoverflow.com/questions/4525206/android-audiorecord-class-process-live-mic-audio-quickly-set-up-callback-func
 * 
 * @author Wijnand Warren
 */
public class VoiceInput extends Thread {

	private static final String LOG_TAG = "VoiceInput";
	
	private static final int BUFFER_SIZE = 320;
	private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
	// NOTE: 44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices.
	// TODO: Do a check to see if 8K is available!!
	private static final int SAMPLE_RATE_IN_HZ = 8000;//44100;
	private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	
	private boolean stopped;
	private boolean lastRun;
	private AudioRecord recorder;
	private Vector<byte[]> audioData;
	private int minBufferSize;
	private int totalRawAudioSize;
	
	private byte[] remainderAudioBuffer;
	private int remainderBufferSize;

	private FileOutputStream outputFile;
	
	/**
	 * CONSTRUCTOR
	 */
	public VoiceInput(FileOutputStream outputFile) {
		// TODO: Are we really urgent?
		//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		this.outputFile = outputFile;
		stopped = false;
		lastRun = false;
		recorder = null;
		audioData = new Vector<byte[]>();
		totalRawAudioSize = 0;
		
		remainderAudioBuffer = new byte[GenericVoiceController.ILBC_30_MS_FRAME_SIZE_DECODED];
		remainderBufferSize = 0;
	}
	
	// ==========================
	// PRIVATE METHODS
	// ==========================
	
	/**
	 * Starts recording the microphone.
	 */
	private void startRecorder() {
		Log.i(LOG_TAG, "startRecorder()");
		minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT);
		Log.i(LOG_TAG, "Buffer size: " + minBufferSize);
		
		// 480 bytes for 30ms(y mode)
        int truncated = minBufferSize % GenericVoiceController.ILBC_30_MS_FRAME_SIZE_DECODED;
        if (truncated != 0) {
        	minBufferSize += GenericVoiceController.ILBC_30_MS_FRAME_SIZE_DECODED - truncated;
            Log.i(LOG_TAG, "Extending buffer to: " + minBufferSize);
        }
		
		recorder = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize * 10);
		recorder.startRecording();
	}

	
	// ==========================
	// PUBLIC METHODS
	// ==========================
	
	/**
	 * Retrieves all recorded audio data.
	 * 
	 * @return The byte array containing all recorded audio data.
	 */
	public byte[] getAudioData() {
		Log.i(LOG_TAG, "getAudioData()");
		byte[] data = new byte[audioData.size() * BUFFER_SIZE];
		Log.i(LOG_TAG, "Expected data size: " + data.length);
		
		for (int i = 0; i < audioData.size(); i++) {
			byte[] buffer = audioData.elementAt(i);
			System.arraycopy(buffer, 0, data, i * BUFFER_SIZE, BUFFER_SIZE);
		}
		
		return data;
	}
	
	/**
	 * Calculates the duration of the recording based on the buffer size.
	 * 
	 * @return Record duration in seconds.
	 */
	public float getDurationBasedOnBufferLength() {
		float duration = 0;
		
		duration = totalRawAudioSize / 2; // 16 bit pcm
		duration = duration / SAMPLE_RATE_IN_HZ;
		
		return duration;
	}
	
	/**
	 * Flags this Thread to be stopped.
	 */
	public void requestStop() {
		Log.i(LOG_TAG, "requestStop()");
		stopped = true;

		// Stop recording.
		recorder.stop();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {
			while(true) {
				final byte[] recorderSamples;
                final byte[] encodedData;
                final int bytesEncoded;
                int bytesToEncode;
                byte[] tempSamples;
                
                recorderSamples = new byte[minBufferSize];
                int recorderSampleSize = 0;
                
                // Calculation taken from iLBC Codec.encode()
                int estimatedEncodedDataLength = minBufferSize / GenericVoiceController.ILBC_30_MS_FRAME_SIZE_DECODED * GenericVoiceController.ILBC_30_MS_FRAME_SIZE_ENCODED;
                encodedData = new byte[estimatedEncodedDataLength];
                
                // Read from AudioRecord buffer.
                recorderSampleSize = recorder.read(recorderSamples, 0, minBufferSize);
                
                // Error checking:
                if (recorderSampleSize == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(LOG_TAG, "read() returned AudioRecord.ERROR_INVALID_OPERATION");
                } else if (recorderSampleSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(LOG_TAG, "read() returned AudioRecord.ERROR_BAD_VALUE");
                } else if (recorderSampleSize == AudioRecord.ERROR) {
                    Log.e(LOG_TAG, "read() returned AudioRecord.ERROR");
                }
                
                // Making sure we always pass multiples of 480 to the encoder:
                bytesToEncode = recorderSampleSize + remainderBufferSize;
                
                // Calculate what the size of the new remainder should be.
                int newRemainderBufferSize = bytesToEncode % GenericVoiceController.ILBC_30_MS_FRAME_SIZE_DECODED;
                bytesToEncode -= newRemainderBufferSize;
                
            	// Final bit to encode.
            	if(lastRun) {
            		Log.i(LOG_TAG, "Encoding the last piece of data!");
            		bytesToEncode = GenericVoiceController.ILBC_30_MS_FRAME_SIZE_DECODED;
            		newRemainderBufferSize = 0;
            		estimatedEncodedDataLength = GenericVoiceController.ILBC_30_MS_FRAME_SIZE_ENCODED;
            		tempSamples = new byte[bytesToEncode];
            		
            		// Copy all sample data
            		System.arraycopy(recorderSamples, 0, tempSamples, remainderBufferSize, recorderSampleSize);
            		
            		// Pad last sample with zeroes.
            		int paddingStart = remainderBufferSize + recorderSampleSize;
            		int paddingEnd = bytesToEncode - 1;
            		Log.i(LOG_TAG, "Padding from: " + paddingStart + " to: " + paddingEnd +" with 0-bytes." );
            		Arrays.fill(tempSamples, paddingStart, paddingEnd, (byte)0);
            	} else {
            		tempSamples = new byte[bytesToEncode];
            		
            		// Grab bytes from previous remainder, if any.
                    if(remainderBufferSize > 0) {
                    	System.arraycopy(remainderAudioBuffer, 0, tempSamples, 0, remainderBufferSize);
                    }
            		
	            	// Copy all data if no remainder needed.
	            	if(newRemainderBufferSize == 0) {
	                	Log.w(LOG_TAG, "No remainder! :D");
	                	
	                	System.arraycopy(recorderSamples, 0, tempSamples, remainderBufferSize, recorderSampleSize);
	                	remainderBufferSize = 0;
	                } 
	            	// Remainder needed to please ILBC encoder.
	            	else {
	                	Log.w(LOG_TAG, "Found a remainder: " + bytesToEncode + ", %480: " + newRemainderBufferSize);
	                	// Grab up to multiple of 480 from samples.
	                	int copyLength = recorderSampleSize - newRemainderBufferSize - remainderBufferSize;
	                	System.arraycopy(recorderSamples, 0, tempSamples, remainderBufferSize, copyLength);
	                	
	                	// Stick rest in remainder
	                	if(newRemainderBufferSize > 0) {
	                		System.arraycopy(recorderSamples, copyLength, remainderAudioBuffer, 0, newRemainderBufferSize);
	                		remainderBufferSize = newRemainderBufferSize;
	                	}
	                }
            	}
                
            	// Keep track of total recorded bytes for duration calculation.
            	totalRawAudioSize += bytesToEncode;
            	
                // Actual encoding.
            	// TODO: Fiddle with noise suppression.
                bytesEncoded = Codec.instance().encode(tempSamples, 0, bytesToEncode, encodedData, 0);
                
                try {
                	outputFile.write(encodedData, 0, bytesEncoded);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to write audio data.");
                }
                
                // Final checks:
                if(stopped && !lastRun && newRemainderBufferSize > 0) {
                	lastRun = true;
				} else if(lastRun || (stopped && newRemainderBufferSize == 0)) {
                	break;
                }
			}
		} catch(Throwable e) {
			Log.e(LOG_TAG, "Error reading voice audio", e);
		} finally {
			// Clean up
			Log.i(LOG_TAG, "VoiceInput Thread is stopped.");
			recorder.release();
			recorder = null;
			
			// Reset encoder
			Codec.instance().resetEncoder();
			
			// Close file.
			try {
				outputFile.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, "Failed to close outputFile!");
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() {
		remainderBufferSize = 0;
		stopped = false;
		lastRun = false;
		totalRawAudioSize = 0;
		
		startRecorder();
		super.start();
	}
	
}
