package com.neueda.__Code_Titans.repo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import com.neueda.__Code_Titans.entity.Accounts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@SuppressWarnings("unused")
public class AccountRepo {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Get all accounts
    public List<Accounts> findAll() {
        String sql = """
                SELECT account_id, account_number, account_holder_name, bank_name, balance, currency, account_status, created_at
                FROM accounts
                """;
        return jdbcTemplate.query(sql, accountRowMapper());
    }

    // Get one account by ID
    public Optional<Accounts> findById(Long accountId) {
        String sql = """
                SELECT account_id, account_number, account_holder_name, bank_name, balance, currency, account_status, created_at
                FROM accounts
                WHERE account_id = ?
                """;
        return jdbcTemplate.query(sql, accountRowMapper(), accountId)
                .stream()
                .findFirst();
    }

    // Get one account by account number
    public Optional<Accounts> findByAccountNumber(String accountNumber) {
        String sql = """
                SELECT account_id, account_number, account_holder_name, bank_name, balance, currency, account_status, created_at
                FROM accounts
                WHERE account_number = ?
                """;
        return jdbcTemplate.query(sql, accountRowMapper(), accountNumber)
                .stream()
                .findFirst();
    }

    // Check if account number already exists
    public boolean existsByAccountNumber(String accountNumber) {
        String sql = "SELECT COUNT(1) FROM accounts WHERE account_number = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, accountNumber);
        return count != null && count > 0;
    }

    // Save a new account and return it with generated ID
    public Accounts save(Accounts account) {
        String sql = """
                INSERT INTO accounts (account_number, account_holder_name, bank_name, balance, currency, account_status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"account_id"});
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getAccountHolderName());
            ps.setString(3, account.getBankName());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getCurrency());
            ps.setString(6, account.getAccountStatus() != null ? account.getAccountStatus() : "ACTIVE");
            return ps;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        if (generatedId != null) {
            account.setAccountId(generatedId.longValue());
        }
        return account;
    }

    private RowMapper<Accounts> accountRowMapper() {
        return (ResultSet rs, int rowNum) -> {
            Accounts account = new Accounts();
            account.setAccountId(rs.getLong("account_id"));
            account.setAccountNumber(rs.getString("account_number"));
            account.setAccountHolderName(rs.getString("account_holder_name"));
            account.setBankName(rs.getString("bank_name"));
            account.setBalance(rs.getBigDecimal("balance"));
            account.setCurrency(rs.getString("currency"));
            account.setAccountStatus(rs.getString("account_status"));
            account.setCreatedAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime()
                    : null);
            return account;
        };
    }
}
