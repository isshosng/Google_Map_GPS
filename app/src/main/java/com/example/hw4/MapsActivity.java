package com.example.hw4;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    // GoogleMap 객체의 이벤트 및 사용자 상호작용을 처리하는 콜백 인터페이스입니다.

    private final String ARG_MARKER_OPTIONS = "ARG_MARKER_OPTIONS";

    // 위치 정보 엑세스 권한 요청
    private final ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseLocationGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                // 정확한 위치를 기기로부터 얻으려면 위의 두 permission을 manifest에 등록하고 해당 Activity에도 적어준다

                if (fineLocationGranted != null && fineLocationGranted) {
                    // Precise location access granted
                    startLocationUpdates();

                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    startLocationUpdates();
                    // Only approximate location access granted

                } else {
                    Toast.makeText(this,
                            "Unable to launch app because location permissions are denied.",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                }
            }
    );


    private GoogleMap map; // GoogleMap 변수 선언
    // 기본 지도 기능 및 데이터를 관리하기 위한 진입점입니다. 앱은 SupportMapFragment 또는 MapView 객체에서 검색한 GoogleMap 객체에만 액세스할 수 있습니다.
    private TextView logTextView; // TextView 변수 선언
    private NestedScrollView logScrollView; // NestedScrollView  변수 선언
    // ScrollView Class를 사용하면 스크롤을 맨 밑에서 내리면 다시 올라오는데 NestedScrollView Class는 그러지 않음

    private FusedLocationProviderClient fusedLocationClient;
    // 이 기능을 사용하기 위해서 manifest에서 ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION 권한을 선언해준다.
    // 권한을 선언하지 않을 경우 정확성이 매우 떨어지게 된다.

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private Location lastLocation;

    private ArrayList<MarkerOptions> markerOptions = new ArrayList<>();
    // ArrayList 생성. Arraylist는 배열과 달리 크기가 가변적으로 변함. 용량을 초과하면 자동으로 부족한 크기만큼 용량이 늘어남.
    // 사용 중인 공간의 크기가 size임.
    // MarkerOptions 타입으로 선언했으며 해당 타입의 데이터만 추가가 가능함

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // 위치 서비스 클라이언트 생성


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // FusedLocationProviderClient로 위치 데이터를 요청함

        // 위치정보 요청
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        /* setInterval : 해당 method는 앱에서 선호하는 위치 업데이트 수신 간격을 밀리초 단위로 설정합니다.
         위치 업데이트는 배터리 사용량을 최적화하기 위해 설정된 간격보다 다소 빠르거나 느릴 수 있고 아예 업데이트가 없을 수 있습니다 */
        locationRequest.setFastestInterval(5000);
        /* setFastestInverval : 해당 method는 앱이 위치 업데이트를 처리할 수 있는 가장 빠른 간격을 밀리초 단위로 설정합니다.
        앱이 setInterval()에 지정된 간격보다 빠르게 업데이트를 수신하는 경우가 아니라면 호출할 필요가 없음
         */
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        /* setPriority : 해당 method는 요청의 우선순위를 설정해주는데 PRIORITY_HIGH_ACCURACY로 설정하면 가장 정확한 위치를 요청하게 되고
        GPS를 사용하여 위치를 확인할 가능성이 높음
         */
        // 위치서비스에 데이터를 요청할 객체를 생성함
        // 정확도와 인터벌 시간을 정해줌
        // 우선순위는 int

        // FusedLocationProviderClient와 LocationRequest를 생성하여 위치 정보를 요청하면
        // LocationCallback Class의 onLocationResult method가 호출되어 위치 정보가 갱신된다
        // FusedLocationProviderClient에서 마지막에 저장된 위치 정보를 가져올 수 있습니다.




        // 시스템에 의해 앱이 종료된 경우, Marker 를 되살리기 위함
        if (savedInstanceState != null) {
            markerOptions = savedInstanceState.getParcelableArrayList(ARG_MARKER_OPTIONS);
            // savedInstanceState는 변경된 값을 유지할 경우 쓰이는데 null 값이 아닐 경우 onCreate method에서 값을 가져다 쓸 수 있다.

            if (markerOptions == null) {
                markerOptions = new ArrayList<>();
            }
        }

        // UI 초기화
        initUi();
    } /*이 콜백은 시스템이 먼저 활동을 생성할 때 실행되는 것으로, 필수적으로 구현해야 합니다.
    활동이 생성되면 생성됨 상태가 됩니다. onCreate() 메서드에서 활동의 전체 수명 주기 동안 한 번만 발생해야 하는 기본 애플리케이션 시작 로직을 실행합니다.
      */
    /**
     * UI 초기화 함수
     */
    private void initUi() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        // GoogleMap 객체의 수명 주기를 관리하기 위한 프래그먼트입니다.
        assert mapFragment != null;
        // assert는 파라미터가 제대로 넘어왔는지, 계산이 제대로 됐는지 혹은 특정 메소드가 작동하는 한계상황(Null이 들어오면 작동안함)을 정하는 용도로 사용한다.
        mapFragment.getMapAsync(this);
        // SupportMapFragment를 통해 레이아웃에 만든 fragment의 ID를 참조하고 Map을 연결
        // getMapAsync()는 반드시 Main Thread에서 호출돼야함


        // 마커 추가 버튼 리스너 등록
        findViewById(R.id.add_marker_button).setOnClickListener(v -> {
            if (lastLocation == null) {
                Toast.makeText(this,
                        "Importing current location. Please try again.",
                        Toast.LENGTH_SHORT
                ).show(); // 위치정보가 뜨지 않을때 Toast message 친절하게 알려주기
                return;
            }

            LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            // latLng에 경위도를 넣어줌

            if (addMarker(latLng) != null) {
                try {
                    log(); // 로그 파일 생성
                } catch (IOException e) {
                    e.printStackTrace();
                    // 에러 메세지의 발생 근원지를 찾아서 단계별로 에러를 출력한다.
                }
            }
        });


        // TextView, ScrollView 생성
    }

    /**
     * 위치 권한이 부여되었는지 확인하는 함수.
     * 위치 권한이 없다면 권한 요청, 권한이 있는 경우 사용자의 위치를 실시간으로 받기 위해 startLocationUpdates 호출
     */
    private void checkLocationPermission() {
        boolean coarseLocationGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean fineLocationGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        // Before you perform the actual permission request, check whether your app
        // already has the permissions, and whether your app needs to show a permission
        // rationale dialog.
        if (!coarseLocationGranted && !fineLocationGranted) {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            startLocationUpdates();// 권한이 부여된 경우 사용자의 위치를 실시간으로 받음
        }
    }

    /**
     * 위치 업데이트 요청 함수
     * locationCallback 이 null 이 아닌 경우는 이미 업데이트 요청을 한 상태이다.
     */
    @SuppressLint("MissingPermission") // 권한 재확인 방지. MissingPermission 항목을 건너뛴다
    private void startLocationUpdates() {
        if (locationCallback != null) return;

        locationCallback = new LocationCallback() {
            @Override
            // LocationCallback을 생성하고 onLocationResult(위치정보 변경 시 호출됨) method를 Overriding
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // null을 허용하지 않을 경우 사용하는 Nullness Annotation
                lastLocation = locationResult.getLastLocation(); // 마지막으로 알려진 위치 요청
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper()); // messagequeue에 들어오는 message들을 차례대로 handler로 전달함
    } // Interval마다 계속 현재 위치 데이터를 요청하므로 현재 위치를 주기적으로 체크할 때 requestLocationUpdates를 사용함

    /**
     함 위치 업데이트 중지 함수
     * locationCallback 이 null 인 경우는 이미 업데이트를 중지했거나, 요청을 하지 않은 상태이다.
     */
    private void stopLocationUpdates() {
        if (locationCallback == null) return; // locationCallback이 null일때 리턴함

        fusedLocationClient.removeLocationUpdates(locationCallback); // 업데이트 중지
        locationCallback = null;
        lastLocation = null;
    }

    /**
     * 마커를 추가하는 함수
     *
     * @param latLng 마커가 추가될 위치
     * @return 추가된 마커, null 이면 마커 추가 실패
     * activity_maps.xml에서 FrameLayout 아래에
     */
    private Marker addMarker(LatLng latLng) {
        if (map == null) return null;

        // GoogleMap에 표시할 Marker에 대한 옵션 설정
        MarkerOptions options = new MarkerOptions();

        options.position(latLng); // 마커는 현재 위치에 표현됨
        options.title("" + markerOptions.size() + 1); // 마커에 텍스트는 공백으로
        Marker marker = map.addMarker(options); // 마커를 맵 위에 생성
        // 마커는 Marker 유형의 객체이며, GoogleMap.addMarker(markerOptions) 메서드를 통해 지도에 추가됨



        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        // moveCamera method는 해당 화면으로 바로 이동시켜주며, animateCamera method는 화면 전환이 부드러운 특징이 있다


        markerOptions.add(options);
        // ArrayList의 값을 추가하기 위해서는 add() 메서드를 사용함. Arraylist 마지막에 options 데이터를 추가하는 것임

        return marker; // void 형태가 아닐 경우엔 반드시 r함
    }

    /**
     * 로깅 함수
     *
     * @throws IOException
     */
    private void log() throws IOException { // 예외처리
        if (markerOptions.size() == 0) return;

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log.txt");
        // getExternalFilesDir method는 private한 directory에 저장할 때 쓰는 method

        if (markerOptions.size() == 1) {
            if (file.exists()) {
                file.delete(); // 파일 삭제
            } // 안에 내용물이 있으면 지우고 새로 만듦

            file.createNewFile(); // 파일 생성

        } else {
            if (!file.exists()) {
                file.createNewFile(); // 파일 생성
            }
        }

        String dateTime = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date());
        // 받는 데이터 형식을 나타냄.
        LatLng lastPosition = markerOptions.get(markerOptions.size() - 1).getPosition();

        String currentLocation = "[" +dateTime + "]"+"\ncurrent position : " + lastPosition.latitude + ", " + lastPosition.longitude + "\n";
        // 로그 화면에 나오는 텍스트를 다음과 같이 설정함.

        FileOutputStream outputStream = new FileOutputStream(file, true);
        // 기존에 있는 파일에 새로 넣을 내용을 append 할지 여부를 묻는데, true를 해주면 새로 넣게 되고 true를 지정해주지 않으면 기존 내용을 지우고 넣게 된다.

        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        // Data의 표시 대상 지정 outputStream 에 문자를 출력한다.

        /*
        OutStream 클래스의 경우는 byte 단위의 읽기와 쓰기에 사용된다.
        여기서 문제는 Java의 Char 와 String의 타입의 경우 Characters로 취급 되어진다.
        이말은 Char / String를 저장하려면 char 단위로 읽고 쓰는 Reader 와 Writer를 사용해야 한다.
        Java는 이 문제를 해결하기 위해서 byte 단위로 데이터를 읽어 Char 형태로 변화 시켜 연결 고리 역할을 하는 Object를 만들어 놓았다.
        그것이 OutputStreamWriter 이다.

         */

        writer.append(currentLocation);
        // 기존 문자열 뒤에 현재위치 문자열을 추가함

        if (markerOptions.size() > 1) { // 마커가 2개 이상일 경우
            float totalDistance = 0f; // 전체거리 선언
            float[] results = new float[1]; // 결괏값 선언

            for (int i = 0; i < markerOptions.size(); i++) {
                /* 객체 markerOptions는 Arraylist를 받고 있는데, 여기서 차지하는 공간이 size임
                   차지하는 공간은 계속해서 하나씩 증가
                */
                if (i + 1 >= markerOptions.size()) { // 공간의 크기가 i+1개가 되면 break
                    break;
                }

                LatLng position = markerOptions.get(i).getPosition(); // 처음 위치
                LatLng nextPosition = markerOptions.get(i + 1).getPosition(); // 다음 위치
                // 두 지점간 거리 계산
                Location.distanceBetween(position.latitude, position.longitude,
                        nextPosition.latitude, nextPosition.longitude, results);

                // 이전 마커와의 거리 Logging
                if (i + 1 == markerOptions.size() - 1) {
                    String distance = String.format(Locale.getDefault(), "Distance from previous marker : %.3fM\n", results[0]);
                    // 이전 마커와의 거리 변수 선언
                    writer.append(distance); // 기존 문자열 뒤쪽에 거리를 추가함
                }

                totalDistance += results[0]; // 총 이동 거리 계산
            }

            String distance = String.format(Locale.getDefault(), "Total distance traveled to now : %.3fM\n", totalDistance);
            writer.append(distance); // 기존 문자열 뒤쪽에 거리 추가
        }

        writer.flush();
        writer.close();
        // 위 두 가지 함수를 통해 메모리를 아낄 수 있음. Buffer 클래스 사용 후 주로 쓰임

        printLog(); // 로그 출력
    }

    /**
     * 로그를 출력하는 함수
     *
     * @throws IOException
     */
    private void printLog() throws IOException { // 예외처리
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log.txt");
        // 외부파일(로그)의 저장경로를 /storage/sdcard0/Android/data/package/files/DOCUMENTS로 설정하고
        // 파일이름과 확장자는 log.txt로 설정한다

        // Stream을 사용하여 파일에 엑세스
        FileInputStream inputStream = new FileInputStream(file);
        // 주어진 FILE 객체가 가리키는 파일을 stream으로 읽기 위한 객체 생성
        // FileInputStream 클래스를 통해 다른 입력 클래스와 연결해서 파일에서 데이터를 읽거나 쓸 수 있게 함
        // 파일의 내용을 문자로 읽게 함

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        // 해당 파일에 2byte 값을 가진 텍스트를 입력해주는 역할
        /*
        InputStream 클래스의 경우는 byte 단위의 읽기와 쓰기에 사용된다.
        여기서 문제는 Java의 Char 와 String의 타입의 경우 Characters로 취급 되어진다.
        이말은 Char / String를 저장하려면 char 단위로 읽고 쓰는 Reader 와 Writer를 사용해야 한다.
        Java는 이 문제를 해결하기 위해서 byte 단위로 데이터를 읽어 Char 형태로 변화 시켜 연결 고리 역할을 하는 Object를 만들어 놓았다.
        그것이 InputStream 이다.

         */



        logTextView.setText("");
        // 첫 화면은 빈칸으로

        String line = reader.readLine();
        // reader 객체에 있는 텍스트를 readLine 메서드를 통해 한 줄씩 읽어들여 String 형태의 line 객체에 저장


        while (line != null) {
            logTextView.append(line + "\n");
            line = reader.readLine();
        } // 차례대로 한줄씩 로그에 저장한다

        // 스크롤을 맨 마지막으로 이동
        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));

        reader.close();
        // Buffer를 사용한 클래스는 항상 끝에서 close를 해주어야 메모리 관리에 효과적임
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelableArrayList(ARG_MARKER_OPTIONS, markerOptions);
        // putParcelableArrayList는 String을 Arraylist에 넣어줌
        super.onSaveInstanceState(outState);
    }  /* 활동 상태 저장을 해주는 기능으로 기기 구성 변경으로 인해 현재 활동이 제거될 수 있으므로 앱이 활동을 다시 만드는 데 필요한 정보를 저장해야 한다.
         이렇게 하는 한 가지 방법은 Bundle 객체에 저장된 인스턴스 상태를 사용하는 것
         onSaveInstanceState() 콜백을 사용하여 인스턴스 상태를 저장하는 방법이다.
      */

    @Override
    protected void onResume() {
        super.onResume();

        checkLocationPermission();
    } // 위치정보를 요청하고 처리하는 작업은 배터리 소모가 큰 작업이므로 화면이 보이는 시점인 onResume에서 작업을 시작

    @Override
    protected void onPause() {
        stopLocationUpdates();
        super.onPause();
    } // 화면이 사라지는 시점인 onPause에서 콜백 리스너를 해제함. Activity가 어떤 이벤트가 발생해서 앱에서 포커스가 떠날 때 콜백을 호출.

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // method가 null일 경우 경고
        map = googleMap;
        map.clear(); // ArrayList의 모든 값을 삭제할 때 사용


        double lon = 126.65304444;
        double lat = 37.44965000; // 초기 위치가 시드니로 되어 있던 걸 인하대로 설정했다.

        // 초기위치 설정
        LatLng inhaPos = new LatLng(lat, lon);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(inhaPos, 15));

        // 시스템에 의해 앱이 종료된 경우, Marker 를 되살리기 위함
        if (!markerOptions.isEmpty()) {
            ArrayList<MarkerOptions> options = markerOptions;
            markerOptions.clear();

            for (MarkerOptions o : options) {
                addMarker(o.getPosition());
            }

            try {
                printLog();
            } catch (IOException e) {
                e.printStackTrace();
                // 에러 메세지의 발생 근원지를 찾아서 단계별로 에러를 출력한다.
            }
        }
    }
}
