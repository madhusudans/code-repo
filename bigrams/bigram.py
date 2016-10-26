#!/usr/bin/env python
#  
#  Filename: bigram.py
#  

import argparse
import os.path
import math
import nltk
import string
import sys

nltk.download('punkt')

from nltk.tokenize import word_tokenize
from decimal import *


class Bigrams:
	def __init__(self,trainer_path,file_path):
		
		if ( os.path.isfile(trainer_path) and  os.path.isfile(file_path) ):
			self.trainer_path = trainer_path
			self.file_path = file_path
		else:
			print("Invalid file path")
			sys.exit(1)
		# Initialise lists as empty
		self.unigrams_dictionary = {}
		self.bigrams_dictionary = {}
		# Used to tokenize the paragraph into sentences 
		self.tokenizer = nltk.data.load('tokenizers/punkt/english.pickle')
		
		unigrams = []
		# Open the corpus file
		f = open(self.trainer_path,'r')
		for sentence in self.tokenizer.tokenize(f.read()):
			# Add start/end tag to the sentence
			training_set = "> " + sentence.translate(None, string.punctuation)
			tokens = nltk.word_tokenize(training_set.lower())
			# Formulate the unigrams here
			unigrams.extend(tokens)
		# Close the file pointer	
		f.close()
		# Formulate the dictionaries here
		self.unigrams_dictionary = nltk.FreqDist(unigrams)
		self.bigrams_dictionary = nltk.FreqDist(nltk.bigrams(unigrams))
		self.turing_dictionary = {0:len(self.bigrams_dictionary)}
		# Build a separate dictonary for Good Turing Discounting
		for key in self.bigrams_dictionary.keys():
			bi_gram_count = self.bigrams_dictionary.get(key)
			if ( bi_gram_count in self.turing_dictionary ) :
				value = self.turing_dictionary.get(bi_gram_count)
				self.turing_dictionary[bi_gram_count] = value+1 
			else :
				self.turing_dictionary[bi_gram_count] = 1
				
	def makeBiGram(self,title,option):
	
		print ("\n\n_________________________________"+title+"_________________________________\n\n")
		count = 1
		f = open(self.file_path,'r')
		for sentence in self.tokenizer.tokenize(f.read()):
			print ("----------------------------------Sentence:%d----------------------------------" %(count))
			word_list = []
			words = sentence.translate(None, string.punctuation)
			word_list.extend(nltk.word_tokenize(words.lower()))
			# Looping through the word list
			print ("\t"+" \t|  ".join(word_list))
			for i in word_list :
				tmp_string = ''
				for j in word_list:
					key = (i,j)
					tmp_string = tmp_string + str(self.bi_gram_all(key,option)) + " \t|  "
				print (i +" "+tmp_string+"\n")
			if ( option != 0 ):	 
				prob_sentence = Decimal(self.bi_gram_all(('>',word_list[0]),option))
				# Loop to compute the probability
				for i in range(1,len(word_list) - 1):
					prob_sentence *= Decimal(self.bi_gram_all((word_list[i],word_list[i+1]),option))
				print ("Total Probability of the Sentence-%d is :" %count)
				print (Decimal(prob_sentence))
			count += 1
		f.close()
		
	def getGTCount(self,key):
		
		gt_count = key
		if( key < 11 ): # Assuming that smoothing is not required for c > 10
			# New count = ( key + 1 ) * ( value @ key + 1 ) / (value @ key)
			next_key = key + 1
			gt_count =  ( ( Decimal(next_key) * Decimal(self.turing_dictionary[next_key]) ) / Decimal( self.turing_dictionary[key] ) )
		return (gt_count)

	def bi_gram_all(self,key,option):
		
		b = key[0]
		number = 0
		density = 1
		getcontext().prec = 4
		if ( option == 1 ):
			# Computes just the bigram counts
			number = self.bigrams_dictionary.get(key,0)
					
		elif ( option == 2 ):	#Compute using just the bigram model
			# Computes p(a|b) = p(a,b) / p(b) to the fifth decimal	
			number = Decimal(self.bigrams_dictionary.get(key,0))
			density = Decimal(self.unigrams_dictionary.get(b,0))
		
		elif ( option == 3 ): # Compute using bigram model with add-one smoothing
			# Computes p*(a|b) = (c+1)*(N/(N+V)) to the fifth decimal
			number = Decimal(self.bigrams_dictionary.get(key,0) + 1 )
			density = Decimal(self.unigrams_dictionary.get(b,0) + len(self.unigrams_dictionary))
			
		elif ( option == 4 ): # Compute using Good turing discount
			number = self.getGTCount(self.bigrams_dictionary.get(key,0))
			density = Decimal(self.unigrams_dictionary.get(b,0))
			
		if ( density == 0 ):
			number = 0
			density = 1						
		return (Decimal(number/density))
		
def main():
	
	parser = argparse.ArgumentParser(description='Program to compute probabilities using Bi-grams')
	parser.add_argument('-c','--corpus', help='Corpus file name',required=True)
	parser.add_argument('-t','--test', help='Test file name',required=True)
	args = parser.parse_args()
	bigrams=Bigrams(args.corpus,args.test)
	bigrams.makeBiGram("Bigram model count",1)
	bigrams.makeBiGram("Bigram model without smoothing",2)
	bigrams.makeBiGram("Bigram model with add-one smoothing",3)
	bigrams.makeBiGram("Bigram model with Good-Turing discounting",4)
	return 0

if __name__ == '__main__':
	main()
