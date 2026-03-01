package com.codex.identity_verifier.service;

import com.codex.identity_verifier.model.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.time.Instant;

@Service
public class UserAccountService {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.users-table-name:user-accounts}")
    private String usersTableName;

    @Autowired
    public UserAccountService(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient) {
        this.enhancedClient = enhancedClient;
        this.dynamoDbClient = dynamoDbClient;
    }

    public UserAccount getUserByUsername(String username) {
        DynamoDbTable<UserAccount> userTable = enhancedClient
                .table(usersTableName, TableSchema.fromBean(UserAccount.class));

        try {
            return userTable.getItem(Key.builder().partitionValue(username).build());
        } catch (ResourceNotFoundException ex) {
            createUsersTableIfMissing();
            return userTable.getItem(Key.builder().partitionValue(username).build());
        }
    }

    public UserAccount createUser(String username, String passwordHash, String role) {
        DynamoDbTable<UserAccount> userTable = enhancedClient
                .table(usersTableName, TableSchema.fromBean(UserAccount.class));

        Instant now = Instant.now();
        UserAccount account = UserAccount.builder()
                .username(username)
                .passwordHash(passwordHash)
                .role(role)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            userTable.putItem(account);
        } catch (ResourceNotFoundException ex) {
            createUsersTableIfMissing();
            userTable.putItem(account);
        }

        return account;
    }

    private void createUsersTableIfMissing() {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder()
                    .tableName(usersTableName)
                    .build());
            return;
        } catch (ResourceNotFoundException ignored) {
            // Create below.
        }

        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(usersTableName)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("username")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("username")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        dynamoDbClient.createTable(createTableRequest);
        dynamoDbClient.waiter().waitUntilTableExists(
                DescribeTableRequest.builder().tableName(usersTableName).build()
        );
    }
}
