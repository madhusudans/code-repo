#
# Filename: viterbi.py
# version:  v1.0
#  	

import argparse
import sys, ast

from collections import defaultdict as defdict

class Viterbi(object):
	START = '*Start*'
	STOP = '*Stop*'
	
	def __init__(self):
		self._statelevel = []
		self._transitions = defdict(lambda: defdict(lambda: 0.0))
		self._emissions = defdict(lambda: defdict(lambda: 0.0))
		
	def makeModel(self, stateinp, transition_dict={}, emission_dict={}):
		self._statelevel.append(stateinp)
		
		# create state input transition matrix
		for target_stateinp, prob in transition_dict.items():
			self._transitions[stateinp][target_stateinp] = prob

		# create observation matrix
		for observation, prob in emission_dict.items():
			self._emissions[stateinp][observation] = prob
	
	def probability(self, observations):
		# retrieve probability from the last entry in trellis
		probabs = self._forward(observations)
		return probabs[len(observations)][self.STOP]

	def predict(self, observations):
		# set initial the probabilities and backpointers
		probabs = defdict(lambda: defdict(lambda: 0.0))
		probabs[-1][self.START] = 1.0
		pointers = defdict(lambda: {})
	
		# update the probabilities for each observation
		i = -1
		for i, observation in enumerate(observations):
			for stateinp in self._statelevel:
			
				# calculate probabilities of making a transition
				# from a previous state input to this one and seeing
				# the current observation
				route_probabs = {}
				for prev_stateinp in self._statelevel:
					route_probabs[prev_stateinp] = (
						probabs[i - 1][prev_stateinp] *
						self._transitions[prev_stateinp][stateinp] *
						self._emissions[stateinp][observation])
				# select previous state input with the highest probability
				max_stateinp = max(route_probabs, key=route_probabs.get)
				probabs[i][stateinp] = route_probabs[max_stateinp]
				pointers[i][stateinp] = max_stateinp

		# get the best final state
		curr_stateinp = max(probabs[i], key=probabs[i].get)
	
		# follow the pointers to get the best state sequence
		statelevel = []
		for i in xrange(i, -1, -1):
			statelevel.append(curr_stateinp)
			curr_stateinp = pointers[i][curr_stateinp]
		statelevel.reverse()
		return statelevel

def main():
	
	viterbi = Viterbi()
	viterbi.makeModel(Viterbi.START, dict(H=0.8, C=0.2))
	viterbi.makeModel('H',dict(H=0.7, C=0.3),{1:.2, 2:.4, 3:.4})
	viterbi.makeModel('C',dict(H=0.4, C=0.6),{1:.5, 2:.4, 3:.1})
	inputList = ast.literal_eval( sys.argv[1] )
	print(viterbi.predict(inputList))	
	return 0	

if __name__ == '__main__':
	main()
