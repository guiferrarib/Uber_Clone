package com.ferrariapps.uberclone.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.ferrariapps.uberclone.R;
import com.ferrariapps.uberclone.config.ConfiguracaoFirebase;
import com.ferrariapps.uberclone.helper.UsuarioFirebase;
import com.ferrariapps.uberclone.model.Usuario;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class CadastroActivity extends AppCompatActivity {

    private TextInputEditText campoNome, campoEmail, campoSenha;
    private SwitchCompat switchTipoUsuario;

    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        inicializarComponentes();


    }

    private void inicializarComponentes() {
        campoNome = findViewById(R.id.editCadastroNome);
        campoEmail = findViewById(R.id.editCadastroEmail);
        campoSenha = findViewById(R.id.editCadastroSenha);
        switchTipoUsuario = findViewById(R.id.switchTipoUsuario);
    }

    public void validarCadastroUsuario(View view) {
        String textoNome = campoNome.getText().toString();
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();

        if (!textoNome.isEmpty()) {
            if (!textoEmail.isEmpty()) {
                if (!textoSenha.isEmpty()) {
                    Usuario usuario = new Usuario();
                    usuario.setNome(textoNome);
                    usuario.setEmail(textoEmail);
                    usuario.setSenha(textoSenha);
                    usuario.setTipo(verificaTipoUsuario());

                    cadastrarUsuario(usuario);
                } else {
                    Toast.makeText(this,
                            "Preencha a senha!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this,
                        "Preencha o email!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this,
                    "Preencha o nome!", Toast.LENGTH_SHORT).show();
        }
    }

    public void cadastrarUsuario(Usuario usuario) {
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                try {
                    String idUsuario = task.getResult().getUser().getUid();
                    usuario.setId(idUsuario);
                    usuario.salvar();
                    UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());
                    if (verificaTipoUsuario().equals("P")) {
                        startActivity(new Intent(CadastroActivity.this, PassageiroActivity.class));
                        finish();
                        Toast.makeText(this,
                                "Sucesso ao cadastrar passageiro!", Toast.LENGTH_SHORT).show();
                    } else {
                        startActivity(new Intent(CadastroActivity.this, RequisicoesActivity.class));
                        finish();
                        Toast.makeText(this,
                                "Sucesso ao cadastrar motorista!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String excecao = "";
                try {
                    throw task.getException();
                } catch (FirebaseAuthWeakPasswordException e) {
                    excecao = "Digite uma senha com mais de 6 caracteres não sequencial!";
                } catch (FirebaseAuthUserCollisionException e) {
                    excecao = "Este usuário já está cadastrado!";
                } catch (FirebaseAuthInvalidCredentialsException e) {
                    excecao = "E-mail não é valido, digite um e-mail válido!!";
                } catch (Exception e) {
                    excecao = "Erro ao cadastrar usuário: " + e.getMessage();
                    e.printStackTrace();
                }

                Toast.makeText(CadastroActivity.this, excecao, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String verificaTipoUsuario() {
        return switchTipoUsuario.isChecked() ? "M" : "P";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

}