import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
});

export const getSpaces = async () => {
  const res = await api.get("/spaces");
  return res.data;
};