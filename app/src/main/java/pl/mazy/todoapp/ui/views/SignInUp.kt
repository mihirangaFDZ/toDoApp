package pl.mazy.todoapp.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.kodein.di.compose.localDI
import org.kodein.di.instance
import pl.mazy.todoapp.User
import pl.mazy.todoapp.data.LoginData
import pl.mazy.todoapp.data.local.AccountRep
import pl.mazy.todoapp.data.remote.TDAService
import pl.mazy.todoapp.data.remote.model.request.AuthReq
import pl.mazy.todoapp.data.remote.model.request.SingUpReq
import pl.mazy.todoapp.navigation.Destinations
import pl.mazy.todoapp.navigation.NavController
import java.util.regex.Pattern

@Composable
fun SignUp(navController: NavController<Destinations>){
    val userRepository: AccountRep by localDI().instance()
    var login by remember { mutableStateOf("") }
    var mailU by remember { mutableStateOf("") }
    var passwd by remember { mutableStateOf("") }
    var showErrorL by remember { mutableStateOf(false) }
    var showErrorM by remember { mutableStateOf(false) }
    var showErrorP by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val api: TDAService by localDI().instance()
    val scope = rememberCoroutineScope()

    fun errorM(){
        if (showErrorL){
            errorMessage += " invalid login"
        }
        if (showErrorM){
            errorMessage += " invalid E-Mail"
        }
        if (showErrorP){
            errorMessage += " invalid Passwd"
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            value = login,
            textStyle= TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            isError = showErrorL,
            onValueChange = {
                login = it
                showErrorL = !isValidLogin(it)
                errorMessage = ""
                errorM()
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            label = { Text("Login") },
        )
        OutlinedTextField(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            value = mailU,
            textStyle= TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            isError = showErrorM,
            onValueChange = {
                mailU = it
                showErrorM = !isValidEmail(it)
                errorMessage = ""
                errorM()
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            label = { Text("e-Mail") },
        )
        OutlinedTextField(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            value = passwd,
            textStyle= TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            onValueChange = {
                passwd = it
                showErrorP = !isValidPasswd(it)
                errorMessage = ""
                errorM()
            },
            isError = showErrorP,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,keyboardType = KeyboardType.Password),
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff
                IconButton(onClick = {passwordVisible = !passwordVisible}){
                    Icon(imageVector  = image, contentDescription = "")
                }
            }
        )
        Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
             if((!showErrorL&&!showErrorM&&!showErrorP)&&login!=""&&passwd!=""&&mailU!=""){
                 scope.launch {
                     val token = api.signup(SingUpReq(mailU,login,passwd))
                     if(token != null) {
                         LoginData.logIn(token.login, token.access_token,token.sid)
                         navController.navigate(Destinations.TaskList(0, emptyList()))
                         userRepository.signUpUser(login,passwd,mailU,token.sid)
                     }
                 }
             }
        },modifier = Modifier.padding(top = 120.dp)) {
            Text(text = "Sign Up")
        }
    }
}

@Composable
fun SignIn(navController: NavController<Destinations>,user: User?){
    val userRepository: AccountRep by localDI().instance()
    val api: TDAService by localDI().instance()
    var passwordVisible by remember { mutableStateOf(false) }
    var mail by remember { mutableStateOf("") }
    var passwd by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val options = userRepository.getUsers()
    var userE by remember { mutableStateOf(false) }
    fun signU(){
        scope.launch {
            try {
                val token = api.auth(AuthReq(mail, passwd))
                if (token != null) {
                    LoginData.logIn(token.login, token.access_token, token.sid)
                    if (userRepository.checkExist(mail, passwd)) {
                        userRepository.signUpUser(
                            token.login,
                            passwd,
                            mail,
                            token.sid
                        )
                        navController.navigate(Destinations.TaskList(0, emptyList()))
                    } else {
                        userRepository.signInUser(token.sid)
                        navController.navigate(Destinations.TaskList(0, emptyList()))
                    }
                } else {
                    err = "Problem with sign in"
                }
            }catch (e:Exception){
                err = "connection error"
            }
        }
    }
    LaunchedEffect(options){
        if (user!=null){
            passwd = user.passwd
            mail = user.eMail
            signU()
        }
    }
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Text(
                text = "Local",
                modifier = Modifier
                    .clickable { userE = true },
                color = MaterialTheme.colorScheme.onBackground
            )
            DropdownMenu(
                expanded = userE,
                onDismissRequest = { userE = false }
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = {
                            Text(it.login)
                        },
                        onClick = {
                            mail = it.eMail
                            passwd = it.passwd
                            userE = false
                            signU()
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            value = mail,
            textStyle= TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            onValueChange = { mail = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            label = { Text("eMail") },
        )
        OutlinedTextField(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            value = passwd,
            textStyle= TextStyle(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            onValueChange = {
                passwd = it
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,keyboardType = KeyboardType.Password),
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff
                IconButton(onClick = {passwordVisible = !passwordVisible}){
                    Icon(imageVector  = image, contentDescription = "")
                }
            }
        )
        Button(onClick = {
            if(mail!=""&&passwd!="") {
                signU()
            }
         },modifier = Modifier.padding(top = 120.dp)) {
            Text(text = "Sign In")
        }
        Text(text = err, color = MaterialTheme.colorScheme.error)
        Text(
            text = "Sign Up Now",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .clickable { navController.navigate(Destinations.SignUp) }
                .padding(top = 10.dp))
    }
}

fun isValidLogin(loginStr:String) =
    Pattern
        .compile(
            "[A-Za-z0-9]{4,40}$",
            Pattern.CASE_INSENSITIVE
        ).matcher(loginStr).find()
fun isValidEmail(emailStr: String) =
    Pattern
        .compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE
        ).matcher(emailStr).find()
fun isValidPasswd(passwdStr:String) =
    Pattern
        .compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{4,}$",
            Pattern.CASE_INSENSITIVE
        ).matcher(passwdStr).find()

