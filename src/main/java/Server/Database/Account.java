package Server.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class Account {
	private String username;
	private String password;
	private Statistiche stat;

	public Account(String username, String password) {
		this.username = username;
		this.password = password;
		this.stat = new Statistiche();
	}

	public Account(String username, String password, Statistiche stat) {
		this.username = username;
		this.password = password;
		this.stat = stat;
	}

	@Override
	public String toString() {
		return "Account [username=" + username + ", password=" + password + ", stat=" + stat + "]";
	}

	public String getUsername() {return username;}

	public String getPassword(){return this.password;}
	public Statistiche getStat() {return this.stat;}

	public void setStat(Statistiche stat) {
		this.stat = stat;
	}

}

