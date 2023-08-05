# Food Detector

## Features

- [x] Take a photo and upload it
- [x] OCR the photo
- [x] Send the text to chatGPT and get a summary of what the ingredients are and what effects they may have on your body
- [x] Display it to the user
- [x] Save past queries for reference

## Resources (including code used):
- CameraX API reference tutorial (https://developer.android.com/codelabs/camerax-getting-started)
- MLKit OCR reference tutorial (https://developers.google.com/ml-kit/vision/text-recognition/v2/android)

## Build instructions
1. Clone the repo
2. Open the project in Android Studio
3. Add the following to your `local.properties` file:
    ```
    OPENAI_API_KEY=<your key here>
    ```
4. Build and run the app
5. Take a photo of a food label and see the results!
