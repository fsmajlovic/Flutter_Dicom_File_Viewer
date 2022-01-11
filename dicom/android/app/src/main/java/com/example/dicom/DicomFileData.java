package com.example.dicom;

import android.graphics.Bitmap;

public class DicomFileData {
    public DicomFileData (byte[]  imageByteArray, String patientName) {
        this.imageByteArray = imageByteArray;
        this.patientName = patientName;
    }
    private byte[] imageByteArray;
    private String patientName;

    public String getPatientName() {
        return patientName;
    }


    public byte[] getImageByteArray() {
        return imageByteArray;
    }
}
