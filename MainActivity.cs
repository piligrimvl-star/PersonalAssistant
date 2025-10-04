[Activity(Label = "PersonalAssistant", MainLauncher = true)]
public class MainActivity : Activity
{
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;

    protected override void OnCreate(Bundle savedInstanceState)
    {
        base.OnCreate(savedInstanceState);
        SetContentView(Resource.Layout.activity_main);

        InitializeVoiceRecognition();
        CheckPermissions();
    }

    private void InitializeVoiceRecognition()
    {
        speechRecognizer = SpeechRecognizer.CreateSpeechRecognizer(this);
        var intent = new Intent(RecognizerIntent.ActionRecognizeSpeech);
        intent.PutExtra(RecognizerIntent.ExtraLanguageModel, RecognizerIntent.LanguageModelFreeForm);
        intent.PutExtra(RecognizerIntent.ExtraLanguage, "ru-RU");

        speechRecognizer.SetRecognitionListener(new SpeechRecognitionListener(this));
    }
}