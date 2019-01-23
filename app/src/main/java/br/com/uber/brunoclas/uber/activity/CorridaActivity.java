package br.com.uber.brunoclas.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import br.com.uber.brunoclas.uber.R;
import br.com.uber.brunoclas.uber.config.ConfiguracaoFirebase;
import br.com.uber.brunoclas.uber.helper.UsuarioFirebase;
import br.com.uber.brunoclas.uber.model.Requisicao;
import br.com.uber.brunoclas.uber.model.Usuario;

public class CorridaActivity extends AppCompatActivity implements OnMapReadyCallback {

    //Componente
    private Button buttonAceitarCorrida;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localMotorista;
    private LatLng localPassageiro;
    private Usuario motorista;
    private Usuario passageiro;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseRef;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private String statusRequisicao;
    private boolean requisicaoAtiva;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corrida);

        inicializarCompoentes();

        //Recupera dados do usuario
        if (getIntent().getExtras().containsKey("idRequisicao")
                && getIntent().getExtras().containsKey("motorista")) {

            Bundle extras = getIntent().getExtras();
            motorista = (Usuario) extras.getSerializable("motorista");
            localMotorista = new LatLng(
                    Double.parseDouble(motorista.getLatitude()),
                    Double.parseDouble(motorista.getLongitude())
            );
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");
            verificaStatusRequisicao();

        }

    }

    private void verificaStatusRequisicao() {

        final DatabaseReference requisicoes = firebaseRef.child("requisicoes")
                .child(idRequisicao);
        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Recupera requisicao
                requisicao = dataSnapshot.getValue(Requisicao.class);

                if (requisicoes != null) {

                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng(
                            Double.parseDouble(passageiro.getLatitude()),
                            Double.parseDouble(passageiro.getLongitude())
                    );

                    statusRequisicao = requisicao.getStatus();
                    alteraInterfaceStatusRequisicao(statusRequisicao);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void alteraInterfaceStatusRequisicao(String status) {
        switch (status) {
            case Requisicao.STATUS_AGUARDANDO:
                requisicaoAguardando();

                break;
            case Requisicao.STATUS_A_CAMINHO:
                requisicaoACaminho();
                break;
        }
    }

    private void requisicaoACaminho() {

        buttonAceitarCorrida.setText("A caminho do passageiro");

        //Exibe marcador do motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //Exibe marcador passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Centralizar dois marcadores
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

        //Inicia monitoramento do motorista
        iniciarMonitoramentoCorrida(passageiro, motorista);

    }

    private void iniciarMonitoramentoCorrida(final Usuario passageiro, final Usuario motorista) {

        //Inicializar Geofire
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        //Adicionar circulo passageiro
        final Circle circulo = mMap.addCircle(
                new CircleOptions()
                        .center(localPassageiro)
                        .radius(50) //em metros
                        .fillColor(Color.argb(90, 255, 153, 0))
                        .strokeColor(Color.argb(190, 255, 153, 0))
        );

        final GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(localPassageiro.latitude, localPassageiro.longitude),
                0.05 // em km
        );
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (key.equals(motorista.getId())) {
                    requisicao.setStatus(Requisicao.STATUS_VIAGEM);
                    requisicao.atualizarStatus();

                    //Remover listener
                    geoQuery.removeAllListeners();
                    circulo.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.30);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, espacoInterno)
        );
    }

    private void adicionaMarcadorMotorista(LatLng localizacao, String titulo) {

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
        );

    }

    private void adicionaMarcadorPassageiro(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );

    }

    private void requisicaoAguardando() {

        buttonAceitarCorrida.setText("Aceitar corrida");

        //Exibe marcador do motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

    }

    private void inicializarCompoentes() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Iniciar Corrida");

        buttonAceitarCorrida = findViewById(R.id.buttonAceitarCorrida);

        //Configuracoes iniciais
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Recuperar localizacao do usuario
        recuperarLocalizacaoUsuario();

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(localMotorista, 20)
        );

    }

    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //Recuperar a latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                //Atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                localMotorista = new LatLng(latitude, longitude);

                alteraInterfaceStatusRequisicao(statusRequisicao);

              /*  mMap.clear();
                mMap.addMarker(
                        new MarkerOptions()
                                .position(localMotorista)
                                .title("Meu local")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
                );
                mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(localMotorista, 20)
                ); */

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //Solicitar atualizacoes de loclizacao
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener
            );
        }

    }


    public void aceitarCorrida(View view) {

        //Configura requisicao
        requisicao = new Requisicao();
        requisicao.setId(idRequisicao);
        requisicao.setMotorista(motorista);
        requisicao.setStatus(Requisicao.STATUS_A_CAMINHO);

        requisicao.atualzar();

    }

    @Override
    public boolean onSupportNavigateUp() {
        if (requisicaoAtiva) {
            alerta("Necessario encerrar a requisicao atual!", 1);
        } else {

            Intent i = new Intent(CorridaActivity.this, RequisicoesActivity.class);
            startActivity(i);

        }
        return false;
    }

    private void alerta(String msg, int duracao) {
        Toast.makeText(this, msg, duracao).show();
    }


}
