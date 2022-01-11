package com.example.dicom;

import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.imebra.Age;
import com.imebra.CodecFactory;
import com.imebra.ColorTransformsFactory;
import com.imebra.DataSet;
import com.imebra.Date;
import com.imebra.DrawBitmap;
import com.imebra.Image;
import com.imebra.Memory;
import com.imebra.PatientName;
import com.imebra.PipeStream;
import com.imebra.StreamReader;
import com.imebra.TagId;
import com.imebra.TransformsChain;
import com.imebra.VOILUT;
import com.imebra.ageUnit_t;
import com.imebra.drawBitmapType_t;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "fsmileDicomFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // First thing: load the Imebra library
        System.loadLibrary("imebra_lib");

        super.onCreate(savedInstanceState);

    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                            // Note: this method is invoked on the main thread.
                            if (call.method.equals("getFileData")) {
                                byte[] fileBytes = call.argument("fileBytes");
                                if (fileBytes != null) {
                                    DicomFileData dicomFileData = readDicomFile(fileBytes);
                                    if(dicomFileData != null) {
                                        HashMap<String, Object> returnFileData =
                                                new HashMap<String, Object>();
                                        returnFileData.put("patientName", dicomFileData.getPatientName());
                                        returnFileData.put("imageByteArray", dicomFileData.getImageByteArray());

                                        result.success(returnFileData);
                                    }
                                } else {
                                    result.error("UNAVAILABLE", "Dicom file could not be read.", null);
                                }
                            } else {
                                result.notImplemented();
                            }

                        }

                );
    }


    private DicomFileData readDicomFile( byte[] fileBytes) {
        try {

            CodecFactory.setMaximumImageSize(8000, 8000);

            InputStream stream = new ByteArrayInputStream(fileBytes);

            // The usage of the Pipe allows to use also files on Google Drive or other providers
            PipeStream imebraPipe = new PipeStream(32000);

            // Launch a separate thread that read from the InputStream and pushes the data
            // to the Pipe.
            Thread pushThread = new Thread(new PushToImebraPipe(imebraPipe, stream));
            pushThread.start();

            // The CodecFactory will read from the Pipe which is feed by the thread launched
            // before. We could just pass a file name to it but this would limit what we
            // can read to only local files
            DataSet loadDataSet = CodecFactory.load(new StreamReader(imebraPipe.getStreamInput()));


            // Get the first frame from the dataset (after the proper modality transforms
            // have been applied).
            Image dicomImage = loadDataSet.getImageApplyModalityTransform(0);

            // Use a DrawBitmap to build a stream of bytes that can be handled by the
            // Android Bitmap class.
            TransformsChain chain = new TransformsChain();

            if(ColorTransformsFactory.isMonochrome(dicomImage.getColorSpace()))
            {
                VOILUT voilut = new VOILUT(VOILUT.getOptimalVOI(dicomImage, 0, 0, dicomImage.getWidth(), dicomImage.getHeight()));
                chain.addTransform(voilut);
            }
            DrawBitmap drawBitmap = new DrawBitmap(chain);
            Memory memory = drawBitmap.getBitmap(dicomImage, drawBitmapType_t.drawBitmapRGBA, 4);

            // Build the Android Bitmap from the raw bytes returned by DrawBitmap.
            Bitmap renderBitmap = Bitmap.createBitmap((int)dicomImage.getWidth(), (int)dicomImage.getHeight(), Bitmap.Config.ARGB_8888);
            byte[] memoryByte = new byte[(int)memory.size()];
            memory.data(memoryByte);
            ByteBuffer byteBuffer = ByteBuffer.wrap(memoryByte);
            renderBitmap.copyPixelsFromBuffer(byteBuffer);



            String patientName = loadDataSet
                    .getPatientName(new TagId(0x10,0x10), 0, new PatientName("Undefined", "", ""))
                    .getAlphabeticRepresentation();

            //transforming bitmap to byte array for flutter usage
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            renderBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            renderBitmap.recycle();

            DicomFileData dicomFileData = new DicomFileData(byteArray, patientName);

            return dicomFileData;
        }
        catch(Exception e) {
            AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
            dlgAlert.setMessage(e.getMessage());
            dlgAlert.setTitle("Error");
            dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //dismiss the dialog
                } } );
            dlgAlert.setCancelable(true);
            dlgAlert.create().show();
            String test = "Test";
        }
        return null;
    }
}