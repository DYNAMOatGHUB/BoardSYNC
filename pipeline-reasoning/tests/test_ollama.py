from langchain_ollama import ChatOllama

print("START")

llm = ChatOllama(
    model="qwen3:8b",
    temperature=0
)

print("MODEL READY")

response = llm.invoke("Reply with exactly: WORKING")

print("RESPONSE RECEIVED")

print(response.content)