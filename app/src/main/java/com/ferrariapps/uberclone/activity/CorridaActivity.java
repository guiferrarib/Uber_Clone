package com.ferrariapps.uberclone.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.ferrariapps.uberclone.config.ConfiguracaoFirebase;
import com.ferrariapps.uberclone.helper.Local;
import com.ferrariapps.uberclone.helper.UsuarioFirebase;
import com.ferrariapps.uberclone.model.Destino;
import com.ferrariapps.uberclone.model.Requisicao;
import com.ferrariapps.uberclone.model.Usuario;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ferrariapps.uberclone.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.Objects;

public class CorridaActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private Button buttonAceitarCorrida;
    private FloatingActionButton fabRota;
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
    DatabaseReference requisicoes;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private String statusRequisicao;
    private boolean requisicaoAtiva;
    private Destino destino;
    private ValueEventListener valueEventListenerRequisicoes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corrida);
        inicializarComponentes();

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
        requisicoes = firebaseRef.child("requisicoes").child(idRequisicao);
        valueEventListenerRequisicoes = requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requisicao = snapshot.getValue(Requisicao.class);
                if (requisicao != null) {
                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng(
                            Double.parseDouble(passageiro.getLatitude()),
                            Double.parseDouble(passageiro.getLongitude())
                    );
                    statusRequisicao = requisicao.getStatus();
                    destino = requisicao.getDestino();
                    alteraInterfaceStatusRequisicao(statusRequisicao);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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
            case Requisicao.STATUS_VIAGEM:
                requisicaoViagem();
                break;
            case Requisicao.STATUS_FINALIZADA:
                requisicaoFinalizada();
                break;
            case Requisicao.STATUS_CANCELADA:
                requisicaoCancelada();
                break;
        }
    }

    private void requisicaoCancelada() {
        buttonAceitarCorrida.setEnabled(false);
        Toast.makeText(this,
                "A corrida foi cancelada pelo passageiro!",
                Toast.LENGTH_SHORT).show();
        requisicoes.removeEventListener(valueEventListenerRequisicoes);
        startActivity(new Intent(CorridaActivity.this, RequisicoesActivity.class));
        finish();
    }

    private void requisicaoFinalizada() {

        fabRota.setVisibility(View.GONE);
        buttonAceitarCorrida.setEnabled(false);
        requisicaoAtiva = false;

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        if (marcadorDestino != null)
            marcadorDestino.remove();

        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino, "Destino");
        centralizarMarcardor(localDestino);
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);
        float valor = distancia * 6;
        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);
        buttonAceitarCorrida.setText("Corrida Finalizada - R$ " + resultado);
    }

    private void centralizarMarcardor(LatLng local) {
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(local, 18)
        );
    }

    private void requisicaoAguardando() {
        buttonAceitarCorrida.setText("Aceitar Corrida");
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());
        centralizarMarcardor(localMotorista);
    }


    private void requisicaoACaminho() {
        fabRota.setVisibility(View.VISIBLE);
        buttonAceitarCorrida.setText("A caminho do passageiro");
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);
        iniciarMonitoramento(motorista, localPassageiro, Requisicao.STATUS_VIAGEM);
    }

    private void requisicaoViagem() {
        fabRota.setVisibility(View.VISIBLE);
        buttonAceitarCorrida.setText("A caminho do destino");
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino, "Destino");
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);
        iniciarMonitoramento(motorista, localDestino, Requisicao.STATUS_FINALIZADA);
    }

    private void iniciarMonitoramento(Usuario uOrigem, LatLng localDestino, String status) {
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        Circle circulo = mMap.addCircle(
                new CircleOptions()
                        .center(localDestino)
                        .radius(50)
                        .fillColor(Color.argb(90, 255, 153, 0))
                        .strokeColor(Color.argb(190, 255, 153, 0)));

        GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(localDestino.latitude, localDestino.longitude),
                0.05
        );

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (key.equals(uOrigem.getId())) {
                    requisicao.setStatus(status);
                    requisicao.atualizarStatus();

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
        int espacoInterno = (int) (largura * 0.20);
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, espacoInterno)
        );
    }

    private void adicionarMarcadorMotorista(LatLng localizacao, String titulo) {
        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
        );
    }

    private void adicionarMarcadorPassageiro(LatLng localizacao, String titulo) {
        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );
    }

    private void adicionarMarcadorDestino(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        if (marcadorDestino != null)
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))
        );
    }

    private void inicializarComponentes() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Iniciar Corrida");
        buttonAceitarCorrida = findViewById(R.id.buttonAceitarCorrida);
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        fabRota = findViewById(R.id.fabRota);
        fabRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status = statusRequisicao;
                if (status != null && !status.isEmpty()) {
                    String lat = "";
                    String lon = "";

                    switch (status) {
                        case Requisicao.STATUS_A_CAMINHO:
                            lat = String.valueOf(localPassageiro.latitude);
                            lon = String.valueOf(localPassageiro.longitude);
                            break;

                        case Requisicao.STATUS_VIAGEM:
                            lat = destino.getLatitude();
                            lon = destino.getLongitude();
                            break;
                    }

                    String latLong = lat + "," + lon;
                    Uri uri = Uri.parse("google.navigation:q=" + latLong + "&mode=d");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }
            }
        });

    }

    public void aceitarCorrida(View view) {
        requisicao = new Requisicao();
        requisicao.setId(idRequisicao);
        requisicao.setMotorista(motorista);
        requisicao.setStatus(Requisicao.STATUS_A_CAMINHO);
        requisicao.atualizar();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        recuperarLocalizacaoUsuario();

    }

    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localMotorista = new LatLng(latitude, longitude);
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);
                if (motorista != null) {
                    motorista.setLatitude(String.valueOf(latitude));
                    motorista.setLongitude(String.valueOf(longitude));
                    requisicao.setMotorista(motorista);
                    statusRequisicao = requisicao.getStatus();
                    requisicao.atualizarLocalizacaoMotorista();
                }
                if (!statusRequisicao.equals(Requisicao.STATUS_CANCELADA))
                    alteraInterfaceStatusRequisicao(statusRequisicao);
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    10,
                    locationListener
            );
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        if (requisicaoAtiva) {
            Toast.makeText(this,
                    "Necessário encerrar a corrida atual!", Toast.LENGTH_SHORT).show();
        } else {
            requisicoes.removeEventListener(valueEventListenerRequisicoes);
            locationManager.removeUpdates(locationListener);
            Intent intent = new Intent(CorridaActivity.this, RequisicoesActivity.class);
            startActivity(intent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask();
            }
        }

        if (statusRequisicao != null && !statusRequisicao.isEmpty() && !statusRequisicao.equals(Requisicao.STATUS_AGUARDANDO)) {
            requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
            requisicao.atualizarStatus();
        }

        return false;
    }

}