package com.app.mutants.service;

import static com.app.mutants.utils.ErrorCodes.INVALID_DNA_CHAIN_SIZE;
import static com.app.mutants.utils.ErrorCodes.INVALID_DNA_MATRIX_SIZE;
import static com.app.mutants.utils.ErrorCodes.INVALID_DNA_VALUE;
import static com.app.mutants.utils.MutantConstants.matchSize;
import static com.app.mutants.utils.MutantConstants.validValues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.app.mutants.exceptions.MutantGenericException;
import com.app.mutants.model.Mutant;
import com.app.mutants.pojo.ServiceResponse;
import com.app.mutants.repository.MutantRepository;
import com.mysql.jdbc.CommunicationsException;

@Service
public class MutantServiceImpl implements MutantService {

	@Autowired
	private MutantRepository mutantRepository;
	
	@Override
	@Retryable(value = { CommunicationsException.class }, maxAttempts = 5)
	public ServiceResponse isMutant(List<String> dna) throws MutantGenericException {
		JSONArray jsonArray = new JSONArray(dna);
		List<Mutant> mutantList = mutantRepository.findByDna(jsonArray.toString());
		Mutant mutant = mutantList.isEmpty()?null:mutantList.get(0);
		if(mutant == null) {
			validateDNASize(dna);
			boolean isMutant = mutant(dna);
			mutant = new Mutant(UUID.randomUUID().toString(), jsonArray.toString(), isMutant);
			mutantRepository.save(mutant);
		}			
		return new ServiceResponse(mutant);
	}

	private void validateDNASize(List<String> dna) throws MutantGenericException {
		int matrixColumnSize = dna.size();
		if (matrixColumnSize < 4) {
			throw new MutantGenericException(INVALID_DNA_MATRIX_SIZE, UUID.randomUUID().toString());
		}

		dna.stream().forEach(row -> {
			if (row.length() != matrixColumnSize) {
				throw new MutantGenericException(INVALID_DNA_CHAIN_SIZE, UUID.randomUUID().toString());
			}
		});
	}

	private boolean mutant(List<String> dna) {
		char[][] filledMatrix = fillMatrixArray(dna);
		boolean isMutant = false;
		isMutant = validateDNARow(filledMatrix, dna.size());
		if (!isMutant) {
			isMutant = validateDNACol(filledMatrix, dna.size());
		}
		if (!isMutant) {
			isMutant = validateRightDNADiagonals(filledMatrix, dna.size());
		}
		if (!isMutant) {
			isMutant = validateLeftDNADiagonals(filledMatrix, dna.size());
		}
		return isMutant;
	}

	private char[][] fillMatrixArray(List<String> dna) {
		char[][] dnaMatrix = new char[dna.size()][dna.size()];
		List<Character> validValuesList = Arrays.asList(validValues);
		AtomicInteger i = new AtomicInteger(0);
		dna.stream().forEach(row -> {
			for (int j = 0; j < row.length(); j++) {
				if (!validValuesList.contains(row.charAt(j))) {
					throw new MutantGenericException(INVALID_DNA_VALUE, UUID.randomUUID().toString());
				}
				dnaMatrix[i.get()][j] = row.charAt(j);
			}
			i.addAndGet(1);
		});
		return dnaMatrix;
	}

	private boolean validateDNARow(char[][] dnaMatrix, int size) {
		for (int row = 0; row < size; row++) {
			for (int col = 0; col < size; col++) {
				int subListEnd = col + matchSize;
				if (subListEnd <= size) {
					List<Character> subList = new ArrayList<>();
					for (int subPointer = col; subPointer < subListEnd; subPointer++) {
						subList.add(dnaMatrix[row][subPointer]);
					}
					if (subList.stream().distinct().count() <= 1) {
						return true;
					}
				} else {
					break;
				}
			}
		}
		return false;
	}

	private boolean validateDNACol(char[][] dnaMatrix, int size) {
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				int subListEnd = row + matchSize;
				if (subListEnd <= size) {
					List<Character> subList = new ArrayList<>();
					for (int subPointer = row; subPointer < subListEnd; subPointer++) {
						subList.add(dnaMatrix[subPointer][col]);
					}
					if (subList.stream().distinct().count() <= 1) {
						return true;
					}
				} else {
					break;
				}
			}
		}
		return false;
	}

	private boolean validateRightDNADiagonals(char[][] dnaMatrix, int size) {
		for (int col = 0; col < size; col++) {
			for (int row = 0; row < size; row++) {
				int subListEndRow = row + matchSize;
				int subListEndCol = col + matchSize;
				if (subListEndRow <= size && subListEndCol <= size) {
					List<Character> subList = new ArrayList<>();
					for (int subPointer = 0; subPointer < matchSize; subPointer++) {
						subList.add(dnaMatrix[row + subPointer][col + subPointer]);
					}
					if (subList.stream().distinct().count() <= 1) {
						return true;
					}
				} else {
					break;
				}
			}
		}
		return false;
	}

	private boolean validateLeftDNADiagonals(char[][] dnaMatrix, int size) {
		for (int col = 0; col < size; col++) {
			for (int row = size - 1; row >= 0; row--) {
				if (row - matchSize-1 >= 0 && col + matchSize <= size) {
					List<Character> subList = new ArrayList<>();
					for (int subPointer = 0; subPointer < matchSize; subPointer++) {
						subList.add(dnaMatrix[row - subPointer][col + subPointer]);
					}
					if (subList.stream().distinct().count() <= 1) {
						return true;
					}
				} else {
					break;
				}
			}
		}
		return false;
	}
}
