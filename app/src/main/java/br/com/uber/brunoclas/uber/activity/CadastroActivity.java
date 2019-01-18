package br.com.uber.brunoclas.uber.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
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

public class CadastroActivity extends AppCompatActivity {

    private TextInputEditText campoNome, campoEmail, campoSenha;
    private Switch switchTipoUsuario;

    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);


        //Inicializar Componentes
        campoNome         =  findViewById(R.id.editCadastroNome);
        campoEmail        =  findViewById(R.id.editCadastroEmail);
        campoSenha        =  findViewById(R.id.editCadastroSenha);
        switchTipoUsuario =  findViewById(R.id.switchTipoUsuario);
    }

    public void validarCadastroUsuario(View view
    ){

        //Recuperar textos dos campos
        String textoNome = campoNome.getText().toString();
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();

        if(!textoNome.isEmpty()){
            if(!textoEmail.isEmpty()){
                if(!textoSenha.isEmpty()){

                    Usuario usuario = new Usuario();
                    usuario.setNome(textoNome);
                    usuario.setEmail(textoEmail);
                    usuario.setSenha(textoSenha);
                    usuario.setTipo(verificaTipoUsuario());

                    cadastrarUsuario(usuario);

                }else{
                    alerta("Preenche o senha!", 0);
                }
            }else{
                alerta("Preenche o E-mail!", 0);
            }
        }else{
            alerta("Preenche o nome!", 0);
        }

    }

    private void cadastrarUsuario(final Usuario usuario) {

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    try{
                        String idUsuario = task.getResult().getUser().getUid();
                        usuario.setId(idUsuario);
                        usuario.salvar();

                        UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());

                        if(verificaTipoUsuario() == "P"){
                            startActivity( new Intent(CadastroActivity.this, PassageiroActivity.class));
                            finish();
                            alerta("Sucesso ao cadastrar passageiro!", 0);
                        }else{
                            startActivity( new Intent(CadastroActivity.this, RequisicoesActivity.class));
                            finish();
                            alerta("Sucesso ao cadastrar motorista!", 0);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

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

    public String verificaTipoUsuario(){
        return switchTipoUsuario.isChecked() ? "M" : "P";

    }

    private void alerta(String msg, int duracao){
        Toast.makeText(this, msg, duracao).show();
    }
}
