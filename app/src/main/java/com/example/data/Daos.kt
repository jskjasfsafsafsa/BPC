package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY lastName ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientById(id: Int): Client?

    @Query("SELECT * FROM clients WHERE isBlacklisted = 1")
    fun getBlacklistedClients(): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY id DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: Int): Loan?

    @Query("SELECT * FROM loans WHERE clientId = :clientId ORDER BY id DESC")
    fun getLoansForClient(clientId: Int): Flow<List<Loan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)
}

@Dao
interface RepaymentDao {
    @Query("SELECT * FROM repayments ORDER BY id DESC")
    fun getAllRepayments(): Flow<List<Repayment>>

    @Query("SELECT * FROM repayments WHERE loanId = :loanId ORDER BY id DESC")
    fun getRepaymentsForLoan(loanId: Int): Flow<List<Repayment>>

    @Query("SELECT * FROM repayments WHERE clientId = :clientId ORDER BY id DESC")
    fun getRepaymentsForClient(clientId: Int): Flow<List<Repayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: Repayment): Long

    @Delete
    suspend fun deleteRepayment(repayment: Repayment)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)
}

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents ORDER BY name ASC")
    fun getAllAgents(): Flow<List<Agent>>

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: Int): Agent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: Agent): Long

    @Update
    suspend fun updateAgent(agent: Agent)
}

@Dao
interface CapitalInfoDao {
    @Query("SELECT * FROM capital_info WHERE id = 1 LIMIT 1")
    fun getCapitalFlow(): Flow<CapitalInfo?>

    @Query("SELECT * FROM capital_info WHERE id = 1 LIMIT 1")
    suspend fun getCapital(): CapitalInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapital(capitalInfo: CapitalInfo)
}

@Dao
interface ClientProofDao {
    @Query("SELECT * FROM client_proofs WHERE clientId = :clientId")
    fun getProofsForClient(clientId: Int): Flow<List<ClientProof>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProof(proof: ClientProof): Long

    @Delete
    suspend fun deleteProof(proof: ClientProof)
}
