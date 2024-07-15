package ru.dragonpav.videosaver;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import android.graphics.*;
import android.media.*;
import android.util.*;
import android.text.format.*;
import java.io.*;
import android.content.pm.*;
import android.content.*;

public class MainActivity extends Activity 
{
    private Button addObject;
	private Button startRecording;
	private Button stopRecording;
	private RadioGroup type;
	private NumberPicker npx;
	private NumberPicker npy;
	private NumberPicker sizeNP;
	private SurfaceView sv;
	private SurfaceHolder sh;
	private ArrayList<HashMap<Character, RectF>> al = new ArrayList<>();
	private Paint circlePaint, rectPaint;
	private int selectedObjectIndex;
	private RectF lastRect;
	private boolean firstDrawed, recorderPrepared;
	private MediaRecorder recorder;
	private Thread videoPublisher;
	private boolean flag;
	private Surface persSurf;
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			requestPermissions(new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
		}
		sv = findViewById(R.id.mainSurfaceView);
		sh = sv.getHolder();
		addObject = findViewById(R.id.addObjectButton);
		startRecording = findViewById(R.id.startRecordingButton);
		stopRecording = findViewById(R.id.stopRecordingButton);
		recorder = new MediaRecorder();
		preparePaints();
		sh.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if (!firstDrawed) {
					HashMap<Character, RectF> entry = new HashMap<>();
					float cx = 50;
					float cy = 50;
					float radius = 50;
					RectF rect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
					entry.put('c', rect);
					al.add(entry);
					redraw();
					firstDrawed = true;
				} else {
					redraw();
				}
			}
			@Override
			public void surfaceChanged(SurfaceHolder holder, int width, int height, int format) {}
			public void surfaceDestroyed(SurfaceHolder holder) {}
		});
		sh.setFormat(PixelFormat.RGBA_8888);
		sv.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					for (int i = 0; i < al.size(); i++) {
						HashMap<Character, RectF> e = al.get(i);
						char key = e.keySet().toArray(new Character[] {})[0];
						if (key == 'c') {
							RectF rect = e.get('c');
							if (rect.contains(event.getX(), event.getY())) {
								selectedObjectIndex = i;
							}
						} else if (key == 'r') {
							RectF rect = e.get('r');
							if (rect.contains(event.getX(), event.getY())) {
								selectedObjectIndex = i;
							}
						}
					}
				} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
					if (selectedObjectIndex != -1) {
						HashMap<Character, RectF> e = al.get(selectedObjectIndex);
						char key = e.keySet().toArray(new Character[] {})[0];
						RectF rect = new RectF();
						if (key == 'c') {
							rect = e.get('c');
							float radius = rect.width() / 2;
							rect.left = event.getX() - radius;
							rect.top = event.getY() - radius;
							rect.right = event.getX() + radius;
							rect.bottom = event.getY() + radius;
							al.remove(selectedObjectIndex);
							al.add(selectedObjectIndex, e);
							redraw();
						} else if (key == 'r') {
							rect = e.get('r');
							float rectW = rect.width();
							float rectH = rect.height();
							rect.left = event.getX() - rectW / 2;
							rect.top = event.getY() - rectH / 2;
							rect.right = event.getX() + rectW / 2;
							rect.bottom = event.getY() + rectH / 2;
							al.remove(selectedObjectIndex);
							al.add(selectedObjectIndex, e);
							redraw();
						}
						if (event.getX() < 0 || event.getY() < 0) {
							al.remove(selectedObjectIndex);
							selectedObjectIndex = -1;
							redraw();
						}
						lastRect = rect;
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						if (selectedObjectIndex != -1) {
							HashMap<Character, RectF> e = al.get(selectedObjectIndex);
							char key = e.keySet().toArray(new Character[] {})[0];
							RectF rect = e.get(key);
							if (rect.contains(event.getX(), event.getY())) {
								selectedObjectIndex = -1;
							}
						}
					}
				}
				return true;
			}
		});
		startRecording.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!recorderPrepared) {
					prepareMediaRecorder();
				}
				startMediaRecorder();
			}
		});
		stopRecording.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopMediaRecorder();
			}
		});
		addObject.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final AlertDialog.Builder build = new AlertDialog.Builder(MainActivity.this);
				build.setTitle("Создание объекта");
				LayoutInflater inflater = getLayoutInflater();
				View view = inflater.inflate(R.layout.add_object, null);
				build.setView(view);
				build.setCancelable(false);
				build.setPositiveButton("Создать", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dial, int which) {
						float x = npx.getValue();
						float y = npy.getValue();
						float size = sizeNP.getValue();
						int t = type.getCheckedRadioButtonId();
						if (t == R.id.addCircleRadioButton) {
							RectF rect = new RectF(x - size, y - size, x + size, y + size);
							HashMap<Character, RectF> entry = new HashMap<>();
							entry.put('c', rect);
							al.add(entry);
						} else if (t == R.id.addRectRadioButton) {
							RectF rect = new RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2);
							HashMap<Character, RectF> entry = new HashMap<>();
							entry.put('r', rect);
							al.add(entry);
						}
						redraw();
					}
				});
				build.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dial, int which) {
						dial.dismiss();
					}
				});
				AlertDialog d = build.create();
				type = view.findViewById(R.id.add_objectRadioGroup);
				npx = view.findViewById(R.id.xNumberPicker);
				npy = view.findViewById(R.id.yNumberPicker);
				sizeNP = view.findViewById(R.id.add_objectSeekBar);
				npx.setMaxValue(sh.getSurfaceFrame().width());
				npy.setMaxValue(sh.getSurfaceFrame().height());
				sizeNP.setMaxValue(250);
				d.show();
			}
		});
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater infl = getMenuInflater();
		infl.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		switch (item.getItemId()) {
			case R.id.clearAll:
				al.clear();
				redraw();
				break;
		}
		return true;
	}
	private void prepareMediaRecorder() {
		CharSequence date = DateFormat.format("ddMMyyyy_hhmmss", new Date());
		File out = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) +  "/" + "MOV" + date + ".mp4");
		recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setOutputFile(out);
		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		recorder.setCaptureRate(30);
		recorder.setVideoSize(sh.getSurfaceFrame().width(), sh.getSurfaceFrame().height());
		persSurf = MediaCodec.createPersistentInputSurface();
		recorder.setInputSurface(persSurf);
		try {
			recorder.prepare();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void startMediaRecorder() {
		try {
			recorder.start();
		} catch (Exception e) {
			Toast.makeText(MainActivity.this, "Не удалось запустить запись: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
		flag = true;
		videoPublisher = new Thread(new Runnable() {
			@Override
			public void run() {
				while (flag) {
					final Bitmap bmp = Bitmap.createBitmap(sh.getSurfaceFrame().width(), sh.getSurfaceFrame().height(), Bitmap.Config.ARGB_8888);
					PixelCopy.request(sv, bmp, new PixelCopy.OnPixelCopyFinishedListener() {
						@Override
						public void onPixelCopyFinished(int result) {
							if (result == PixelCopy.SUCCESS) {
								Canvas canv = persSurf.lockCanvas(null);
								canv.drawBitmap(bmp, 0, 0, null);
								persSurf.unlockCanvasAndPost(canv);
							}
						}
					}, new Handler(Looper.getMainLooper()));
					try {
						Thread.sleep(1000L / 30);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}, "VideoPublisher");
		videoPublisher.start();
	}
	private void stopMediaRecorder() {
		flag = false;
		try {
			videoPublisher.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		recorder.stop();
		persSurf.release();
	}
	private void redraw() {
		Canvas c = sh.lockCanvas(null);
		c.drawColor(Color.CYAN);
		for (HashMap<Character, RectF> e : al) {
			char key = e.keySet().toArray(new Character[] {})[0];
			if (key == 'c') {
				RectF rect = e.get('c');
				float cx = rect.centerX();
				float cy = rect.centerY();
				float radius = rect.width() / 2;
				c.drawCircle(cx, cy, radius, circlePaint);
			} else if (key == 'r') {
				RectF rect = e.get('r');
				c.drawRect(rect, rectPaint);
			}
		}
		sh.unlockCanvasAndPost(c);
	}
	private void preparePaints() {
		circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circlePaint.setColor(Color.RED);
		circlePaint.setStyle(Paint.Style.FILL);
		rectPaint = new Paint();
		rectPaint.setColor(Color.BLUE);
		rectPaint.setStyle(Paint.Style.FILL);
	}
}
