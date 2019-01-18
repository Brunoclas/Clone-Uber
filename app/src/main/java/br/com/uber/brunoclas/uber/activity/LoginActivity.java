package br.com.uber.brunoclas.uber.activity;

import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import br.com.uber.brunoclas.uber.R;
import br.com.uber.brunoclas.uber.config.ConfiguracaoFirebase;
import br.com.uber.brunoclas.uber.helper.UsuarioFirebase;
import br.com.uber.brunoclas.uber.model.Usuario;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText campoEmail, campoSenha;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Inicializa compoentes
        campoEmail = findViewById(R.id.editLoginEmail);
        campoSenha = findViewById(R.id.editLoginSenha);

    }

    public void validarLoginUsuario(View view) {

        //Recupera textos dos campos
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();

        if (!textoEmail.isEmpty()) {
            if (!textoSenha.isEmpty()) {
                Usuario usuario = new Usuario();
                usuario.setEmail(textoEmail);
                usuario.setSenha(textoSenha);

                logarUsuario(usuario);

            } else {
                alerta("Preencha o senha!", 0);
            }
        } else {
            alerta("Preencha o email!", 0);
        }

    }

    private void logarUsuario(Usuario usuario) {

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){

                    //Verificar o tipo de usuario logado
                    // "Motorista" / "Passageiro"
                    UsuarioFirebase.redirecionaUsuarioLogado(LoginActivity.this);

                }else{
                    String excessao = "";
                    try{
                        throw task.getException();
                    }catch (FirebaseAuthWeakPasswordException e){
                        excessao = "Digite uma senha mais forte";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        excessao = "Por favor, digite um e-mail válido";
                    }catch (FirebaseAuthUserCollisionException e){
                        excessao = "Essa conta ja foi cadastrada";
                    }catch (Exception e){
                        excessao = "Erro ao cadastrar o usuario: " + e.getMessage();
                        e.printStackTrace();
                    }
                    alerta(excessao, 0);
                }
            }
        });

    }


    private void alerta(String msg, int duracao) {
        Toast.makeText(this, msg, duracao).show();
    }


}
