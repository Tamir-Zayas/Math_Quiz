package com.tamir.mathquiz;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
// Importa la clase Group de ConstraintLayout
import androidx.constraintlayout.widget.Group;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
//manejo de sonido
import android.media.AudioAttributes;
import android.media.SoundPool;

public class MainActivity extends AppCompatActivity {
    // Vistas de la UI
    Button startButton;
    TextView resultTextView;
    TextView pointsTextView;
    TextView sumTextView;
    TextView timerTextView;
    Button playAgainButton;
    // Objeto para agrupar las vistas de los elementos del juego
    Group gameGroup;
    // ArrayList para los botones de respuesta
    ArrayList<Button> answerButtons = new ArrayList<>();

    TextView highScoreTextView;
    // nuevas constantes para SharedPreferences
    public static final String PREFS_NAME = "MathQuizPrefs";
    public static final String HIGH_SCORE_KEY = "highScore";

    // Variables de estado del juego
    ArrayList<Integer> answers = new ArrayList<>();
    int locationOfCorrectAnswer; //Ubicacion de la respuesta correcta
    int score = 0; //Contador de puntos (respuestas correctas)
    int numberOfQuestions = 0;
    CountDownTimer countDownTimer; // Declarar el temporizador como una variable de instancia
    private long millisLeft = 30100; // Valor inicial del tiempo restante (agregar variable)

    //Declaración de variables para sonido
    private SoundPool soundPool;
    private int soundCorrect;
    private int soundWrong;
    private int soundNewRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //Inicialización de las vistas
        startButton = findViewById(R.id.startButton);
        sumTextView = findViewById(R.id.sumTextView);
        resultTextView = findViewById(R.id.resultTextView);
        pointsTextView = findViewById(R.id.pointsTextView);
        timerTextView = findViewById(R.id.timerTextView);
        playAgainButton = findViewById(R.id.playAgainButton);
        highScoreTextView = findViewById(R.id.highScoreTextView);
        gameGroup = findViewById(R.id.gameGroup);

        //Inicializa y añade los botones de respuesta al ArrayList
        answerButtons.add(findViewById(R.id.button0));
        answerButtons.add(findViewById(R.id.button1));
        answerButtons.add(findViewById(R.id.button2));
        answerButtons.add(findViewById(R.id.button3));

        // Listener para los insets de la ventana
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Incicio de configuración de sonidos
        // Configurar los atributos de audio para el juego
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        // Inicializar el SoundPool
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3) // Se puede reproducir hasta 3 sonidos al mismo tiempo
                .setAudioAttributes(audioAttributes)
                .build();
        // Cargar los sonidos desde la carpeta res/raw y obtener sus IDs
        soundCorrect = soundPool.load(this, R.raw.correct_answer, 1);
        soundWrong = soundPool.load(this, R.raw.wrong_answer, 1);
        soundNewRecord = soundPool.load(this, R.raw.new_record, 1);

    }// close void onCreate

    // MÉTODOS DE CICLO DE VIDA DE LA ACTIVIDAD
    @Override
    protected void onPause() {
        super.onPause();
        // Si hay un temporizador corriendo, lo cancelamos.
        // onSaveInstanceState ya habrá guardado el tiempo restante (millisLeft).
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si el juego está en pantalla Y el tiempo no se ha acabado,
        // reanudamos el temporizador desde donde se quedó.
        if (gameGroup.getVisibility() == View.VISIBLE && millisLeft > 0) {
            resumeTimer(millisLeft);
        }
    }

    // MÉTODOS PARA GUARDAR Y RESTAURAR EL ESTADO DEL JUEGO
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Guardar las variables primitivas del estado del juego
        outState.putInt("score", score);
        outState.putInt("numberOfQuestions", numberOfQuestions);
        outState.putInt("locationOfCorrectAnswer", locationOfCorrectAnswer);
        // Guardar los textos de los TextViews que son importantes para el juego
        outState.putString("questionText", sumTextView.getText().toString());
        outState.putString("resultText", resultTextView.getText().toString());
        // Guardar el ArrayList de respuestas
        outState.putIntegerArrayList("answers", answers);
        // Guardar el tiempo restante
        outState.putLong("millisLeft", millisLeft);
        // Guardar la visibilidad de las vistas
        outState.putInt("gameGroupVisibility", gameGroup.getVisibility());
        outState.putInt("startButtonVisibility", startButton.getVisibility());
        outState.putInt("playAgainButtonVisibility", playAgainButton.getVisibility());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restauramos la visibilidad de los elementos del juego
        gameGroup.setVisibility(savedInstanceState.getInt("gameGroupVisibility"));
        startButton.setVisibility(savedInstanceState.getInt("startButtonVisibility"));
        playAgainButton.setVisibility(savedInstanceState.getInt("playAgainButtonVisibility"));
        // Si el juego está en pantalla, restauramos el estado del juego
        if (gameGroup.getVisibility() == View.VISIBLE) {
            // Restauramos las variables primitivas del estado del juego
            score = savedInstanceState.getInt("score");
            numberOfQuestions = savedInstanceState.getInt("numberOfQuestions");
            locationOfCorrectAnswer = savedInstanceState.getInt("locationOfCorrectAnswer");
            // Restauramos los textos de los TextViews que son importantes para el juego
            sumTextView.setText(savedInstanceState.getString("questionText"));
            resultTextView.setText(savedInstanceState.getString("resultText"));
            pointsTextView.setText(String.format(Locale.getDefault(), "%d/%d", score, numberOfQuestions));
            // Restauramos el ArrayList de respuestas
            answers = savedInstanceState.getIntegerArrayList("answers");
            if (answers != null && !answers.isEmpty()) {
                for (int i = 0; i < answers.size(); i++) {
                    answerButtons.get(i).setText(String.valueOf(answers.get(i)));
                }
            }
            // Restauramos el tiempo restante. onResume se encargará de iniciarlo.
            millisLeft = savedInstanceState.getLong("millisLeft");
            // Si el tiempo se había acabado, actualizamos la UI para reflejarlo.
            if (millisLeft <= 0) {
                timerTextView.setText("0s");
                setAnswerButtonsEnabled(false);
            }
        }
    }

    private void loadAndDisplayHighScore() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int highScore = prefs.getInt(HIGH_SCORE_KEY, 0);
        highScoreTextView.setText(String.format(Locale.getDefault(), "Record: %d", highScore));
    }

    /**
     * Inicia el juego, ocultando el botón de inicio y mostrando el grupo de juego.
     */
    public void start(View view) {
        startButton.setVisibility(View.INVISIBLE);
        gameGroup.setVisibility(View.VISIBLE);
        playAgain(null); // Llama a playAgain para configurar e iniciar la primera partida
    }

    // MÉTODOS DEL JUEGO PARA USAR EL TIEMPO RESTANTE (millisLeft)
    /**
     * Configura un nuevo juego, reinicia el temporizador, genera una nueva pregunta y o reinicia la actual.
     */
    public void playAgain(View view) {
        score = 0;
        numberOfQuestions = 0;
        millisLeft = 30100; // Reiniciamos el tiempo para la nueva partida
        // Reinicia los textos de la UI
        timerTextView.setText("30s");
        pointsTextView.setText("0/0");
        resultTextView.setText("");
        // Oculta el botón "Play Again"
        playAgainButton.setVisibility(View.INVISIBLE);
        setAnswerButtonsEnabled(true);
        // Usamos resumeTimer para empezar
        loadAndDisplayHighScore();
        generateQuestion();
        resumeTimer(millisLeft);
    }

    //  Se elimina el método startimer y se usa resumeTimer(long millis)
    //  resumeTimer(long millis): Reinicia el temporizador con el tiempo restante actual.
    public void resumeTimer(long millis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisLeft = millisUntilFinished; // Actualiza la variable en cada tick
                // Actualiza el tiempo en la UI
                timerTextView.setText(String.format(Locale.getDefault(), "%ds", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                // El tiempo ha terminado
                millisLeft = 0;
                timerTextView.setText("0s");
                playAgainButton.setVisibility(View.VISIBLE);
                resultTextView.setText(String.format(Locale.getDefault(), "Tu puntuación: %d/%d", score, numberOfQuestions));
                // Deshabilita los botones de respuesta cuando el tiempo se acaba
                setAnswerButtonsEnabled(false);

                //inicio de la lógica para guardar el record
                //1. Obtener la puntuaación máxima actual guardada
                android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                int highScore = prefs.getInt(HIGH_SCORE_KEY, 0); // el 0 es el valor por defecto si no hay record

                //2. Comparar la puntuación actual con el record
                if (score > highScore) {
                    //nuevo record!
                    resultTextView.setText(String.format(Locale.getDefault(), "New High Score: %d", score));
                    soundPool.play(soundNewRecord, 1, 1, 0, 0, 1); //Reproducir sonido de New Record

                    //3. Guardar el nuevo record
                    android.content.SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(HIGH_SCORE_KEY, score);
                    editor.apply(); // apply() guarda los datos en segundo plano, es más eficiente
                    loadAndDisplayHighScore(); // añadir aquí para actualizar la UI
                } else {
                    resultTextView.setText(String.format(Locale.getDefault(), "Your score: %d/%d", score, numberOfQuestions));
                }
                //Fin de la lógica para guardar el record
            }
        }.start();
    }

    // ... el resto del código (generateQuestion, chooseAnswer, etc.) no necesita más cambios.

    /**
     * Genera una nueva pregunta de suma y actualiza los botones de respuesta.
     */
    public void generateQuestion() {
        Random rand = new Random();
        int a = rand.nextInt(21) + 5; // Genera un número entre 5 y 25
        int b = rand.nextInt(21);

        sumTextView.setText(String.format(Locale.getDefault(), "%d + %d", a, b));
        locationOfCorrectAnswer = rand.nextInt(4);
        answers.clear();

        for (int i = 0; i < 4; i++) {
            if (i == locationOfCorrectAnswer) {
                answers.add(a + b);
            } else {
                int incorrectAnswer = rand.nextInt(46) + 5; // Genera un número entre 5 y 50
                //Si la respuesta correcta es igual a la incorrecta, genera una nueva respuesta
                while (incorrectAnswer == a + b) {
                    incorrectAnswer = rand.nextInt(46) + 5;
                }
                answers.add(incorrectAnswer);
            }
        }

        // Asigna los textos a los botones usando el ArrayList
        for (int i = 0; i < 4; i++) {
            answerButtons.get(i).setText(String.valueOf(answers.get(i)));
        }
    }

    /** Se ejecuta cuando el usuario elige una respuesta. */
    public void chooseAnswer(View view) {
        // Compara el 'tag' del botón presionado con la ubicación de la respuesta correcta
        if (view.getTag().toString().equals(String.valueOf(locationOfCorrectAnswer))) {
            score++;
            resultTextView.setText("Correct!");
            soundPool.play(soundCorrect, 1, 1, 0, 0, 1); // Reproducir sonido de Correct answer
        } else {
            resultTextView.setText("Wrong!");
            soundPool.play(soundWrong, 1, 1, 0, 0, 1); // Reproducir sonido de Wrong answer
        }

        numberOfQuestions++;
        pointsTextView.setText(String.format(Locale.getDefault(), "%d/%d", score, numberOfQuestions));
        generateQuestion();
    }

    /**
     * Método auxiliar para habilitar o deshabilitar todos los botones de respuesta.
     */
    private void setAnswerButtonsEnabled(boolean isEnabled) {
        for (Button button : answerButtons) {
            button.setEnabled(isEnabled);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Liberar memoria utilizada por SoundPool
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}