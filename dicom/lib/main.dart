

import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Dicom File Viewer',
      theme: ThemeData(
        primarySwatch: Colors.teal,
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            primary: Colors.white,
            backgroundColor: Colors.teal[300],
          ),
        ),
      ),
      home: const MyHomePage(title: 'Flutter Dicom File Viewer'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {

  static const dicomFileChannel = MethodChannel("fsmileDicomFile");
  late Uint8List _imageBytes = Uint8List(0);
  late String _patientName = "";
  bool _isImageProcessed = false;


  @override
  Widget build(BuildContext context) {

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: <Widget>[
            Visibility(
                visible: _isImageProcessed,
                child: Column(
                  children: [
                    Padding(padding: const EdgeInsets.fromLTRB(20, 20, 20, 20), child: Image.memory(_imageBytes)),

                    const SizedBox(height: 10, width: 0),
                    Text("Patient name: " + _patientName, style: TextStyle(
                        color: Colors.teal[300],
                        fontWeight: FontWeight.bold
                      ),
                    ),
                  ],
                ),
            ),

            TextButton(
                onPressed: () async {
                  FilePickerResult? result = await FilePicker.platform.pickFiles(withData: true);
                  if (result != null) {
                    _getDicomFileData(result.files.single.bytes!);
                  } else {
                    // User canceled the picker
                  }
                }, child: const Text("Load a Dicom File")
            ),
          ],
        ),
      ),

    );
  }

  Future _getDicomFileData(Uint8List fileBytes) async {
    final arguments = {'fileBytes': fileBytes};
    var fileData = await dicomFileChannel.invokeMethod('getFileData', arguments);
    setState(() {
      _imageBytes = fileData["imageByteArray"];
      _patientName = fileData["patientName"];
      _isImageProcessed = true;
    });
  }

}
