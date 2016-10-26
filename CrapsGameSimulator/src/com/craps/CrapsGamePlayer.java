package com.craps;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
public class CrapsGamePlayer {

	private int rollDice(){
		Random rand = new Random();
		int  dice1 = rand.nextInt(6) + 1;
		int  dice2 = rand.nextInt(6) + 1;
		int sum = dice1+dice2;
		return sum;
	}
	
	private int generateNextEvenBet(){
		return 100;
	}
	
	private int generateNextMartingaleBet
	 (int currentBet,boolean result,int balance){
		if(result){
			return 100;
		} else{
			if(balance - (currentBet*2) >= 0){
				return currentBet*2;
		  } else{
			  return balance;
		  }
		}
	}
	
	private int generateNextReverseMartingaleBet
	 (int currentBet,boolean result,int balance){
		if(!result){
			return 100;
		} else{
			if(balance - (currentBet*2) >= 0){
				return currentBet*2;
		  } else{
			  return balance;
		  }
		}
	}
	
	private void writeOutputToFile(String content){
		try{	
				File currentDirectory = new File(new File(".").getAbsolutePath());
				String path = currentDirectory.getCanonicalPath();
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path+File.separator+"output.txt", true)));
			    out.println(content);
			    out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int generateNewBet(String bettingStrategy,int currentBet,boolean result,int total){
		int bet=0;
		if(("Even").equals(bettingStrategy)){
			bet = this.generateNextEvenBet();
		} else if(("Martingale").equals(bettingStrategy)){
			bet = this.generateNextMartingaleBet(currentBet, result, total);
		}  else if(("Reverse Martingale").equals(bettingStrategy)){
			bet = this.generateNextReverseMartingaleBet(currentBet, result, total);
		}
		
		return bet;
	}
	
	private void playTenGamesOfCraps(String bettingStrategy){
		int bet = 100,diceValue,turn=1,target=0;
		boolean result = true,betToChange = true;
		int total = 1000;
		int totalGames = 0;
		for(int gameNumber=1;gameNumber<=10&&total>0;gameNumber++){
			turn = 1;
			target = 0;
			while(total>0){
				if(betToChange && gameNumber!=1){
					bet = this.generateNewBet(bettingStrategy, bet, result, total);
				}
				diceValue = this.rollDice();
				if(turn == 1){
					if(diceValue == 7 || diceValue == 11){
						total += bet;
						result = true;
						betToChange = true;
						break;
					} else if(diceValue == 2 || diceValue == 3 || diceValue == 12){
						total -= bet;
						result = false;
						betToChange = true;
						break;
					} else {
						target=diceValue;
						betToChange = false;
						turn++;
						continue;
					}
				} 
				else {
					if(diceValue == target){
						total += bet;
						result = true;
						betToChange = true;
						break;
					} else if(diceValue == 7){
						total -= bet;
						result = false;
						betToChange = true;
						break;
					} else {
						betToChange = false;
						turn++;
						continue;
					}
				}
			}
			totalGames = gameNumber;
		}
			
		String content = bettingStrategy+","+totalGames+","+total;
		
		this.writeOutputToFile(content);
	}
	
	private void playARoundOfCraps(int roundNum){ 
		this.writeOutputToFile("Round "+roundNum+": ");
		this.writeOutputToFile("");
		this.writeOutputToFile("Strategy,Number of games,Ending Balance"); 
		this.playTenGamesOfCraps("Even");	
		this.playTenGamesOfCraps("Martingale");	
		this.playTenGamesOfCraps("Reverse Martingale");		
		this.writeOutputToFile("");
	}
	
	public static void main(String[] args) {
		CrapsGamePlayer s = new CrapsGamePlayer();
		for(int rounds=1;rounds<=5;rounds++){
			s.playARoundOfCraps(rounds);
		}
	}
	
}