package com.neueda.__Code_Titans.repo;

import java.sql.ResultSet;
import java.util.Optional;

import com.neueda.__Code_Titans.entity.Accounts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@SuppressWarnings("unused")
public class AccountRepo {

	private final JdbcTemplate jdbcTemplate;

	public AccountRepo(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

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

	public boolean existsByAccountNumber(String accountNumber) {
		String sql = "SELECT COUNT(1) FROM accounts WHERE account_number = ?";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, accountNumber);
		return count != null && count > 0;
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
