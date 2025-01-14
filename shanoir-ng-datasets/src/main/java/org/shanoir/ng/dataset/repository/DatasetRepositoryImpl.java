package org.shanoir.ng.dataset.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatasetRepositoryImpl implements DatasetRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
	@Override
    public List<Object[]> queryStatistics(String studyNameInRegExp, String studyNameOutRegExp,
            String subjectNameInRegExp, String subjectNameOutRegExp) throws Exception {
        
		//"getStatistics" is the name of the MySQL procedure
		StoredProcedureQuery query = entityManager.createStoredProcedureQuery("getStatistics"); 

		//Declare the parameters in the same order
		query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);

		//Pass the parameter values
		query.setParameter(1, studyNameInRegExp);
		query.setParameter(2, studyNameOutRegExp);
		query.setParameter(3, subjectNameInRegExp);
		query.setParameter(4, subjectNameOutRegExp);

		//Execute query
		query.execute();
		
		List<Object[]> results = query.getResultList();
		return results;
    }

}
